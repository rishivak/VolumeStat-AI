package com.algo.ws;

import com.algo.utils.Log;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.*;

import com.algo.engine.UnderlyingContext;
import com.algo.model.DepthLevel;
import com.algo.model.OrderBookSnapshot;
import com.algo.model.OptionMeta;
import com.algo.state.OrderBookState;
import com.algo.analytics.OptionSignalEngine;
import com.algo.analytics.OrderBookCalculator;

import java.util.*;

public class ZerodhaOrderBookWS {

    private KiteTicker ticker;
    // heartbeat limiter (per futures token)
    private final Map<Long, Long> lastHeartbeat = new HashMap<>();

    // futToken -> context
    private final Map<Long, UnderlyingContext> contexts = new HashMap<>();

    // optionToken -> context
    private final Map<Long, UnderlyingContext> optionToContext = new HashMap<>();

    public ZerodhaOrderBookWS(List<UnderlyingContext> ctxList) {
        for (UnderlyingContext ctx : ctxList) {
            contexts.put(ctx.futToken, ctx);
            ctx.options.forEach((token, opt) -> optionToContext.put(token, ctx));
        }
    }

    // =====================================================
    // 🚀 START
    // =====================================================
    public void start(KiteConnect kite, List<Long> tokens) throws KiteException {

        ticker = new KiteTicker(kite.getAccessToken(), kite.getApiKey());

        ticker.setOnConnectedListener(() -> {
            Log.info("WS connected ✅");
            ArrayList<Long> tokenList = new ArrayList<>(tokens);
            ticker.subscribe(tokenList);
            ticker.setMode(tokenList, KiteTicker.modeFull);
        });

        ticker.setOnErrorListener(new OnError() {
            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }

            @Override
            public void onError(KiteException e) {
                System.err.println(e.message);
            }

            @Override
            public void onError(String error) {
                System.err.println(error);
            }
        });

        ticker.setOnTickerArrivalListener(ticks -> {
            for (Tick tick : ticks) handleTick(tick);
        });


        ticker.setTryReconnection(true);
        ticker.setMaximumRetries(10);
        ticker.setMaximumRetryInterval(30);

        ticker.connect();
    }

    // =====================================================
    // 🧠 TICK HANDLER
    // =====================================================
    private void handleTick(Tick tick) {

        if (tick.getMarketDepth() == null) return;

        long token = tick.getInstrumentToken();
        OrderBookSnapshot curr = buildSnapshot(tick);

        // 🔥 update FIRST
        OrderBookState.update(token, curr);

        OrderBookSnapshot prev = OrderBookState.getPrev(token);

        // =====================================================
        // 🚀 FUTURES — NO WARM-UP BLOCK
        // =====================================================
        UnderlyingContext ctx = contexts.get(token);
        if (ctx != null) {

            // prev may be null on first tick → skip aggression only
            if (prev == null) return;

            double pressure = OrderBookCalculator.weightedPressure(curr.bids, curr.asks);

            long aggression = OrderBookCalculator.aggression(prev, curr);

            long now = System.currentTimeMillis();
            long last = lastHeartbeat.getOrDefault(token, 0L);

            if (now - last >= 1000) {
                lastHeartbeat.put(token, now);

                long bid5 = curr.bids.stream().limit(5).mapToLong(b -> b.quantity).sum();
                long ask5 = curr.asks.stream().limit(5).mapToLong(a -> a.quantity).sum();

                double ladderPressure = bid5 / (double) (bid5 + ask5 + 1);

                Log.info(ctx.name + " LADDER | LTP=" + curr.ltp + " bid5=" + bid5 + " ask5=" + ask5 + " ladderP=" + round(ladderPressure));
            }

            return; // ✅ IMPORTANT
        }

        // =====================================================
        // 🎯 OPTIONS — REQUIRE FULL WARM-UP
        // =====================================================
        UnderlyingContext optCtx = optionToContext.get(token);
        if (optCtx == null) return;

        if (prev == null) return; // options must wait

        OptionMeta opt = optCtx.options.get(token);

        OrderBookSnapshot futCurr = OrderBookState.getCurr(optCtx.futToken);
        OrderBookSnapshot futPrev = OrderBookState.getPrev(optCtx.futToken);

        if (futCurr == null || futPrev == null) return;

        OptionSignalEngine.evaluateOption(opt, futPrev, futCurr, prev, curr);
    }

    // =====================================================
    // 🧱 SNAPSHOT
    // =====================================================
    private OrderBookSnapshot buildSnapshot(Tick tick) {

        List<DepthLevel> bids = new ArrayList<>();
        List<DepthLevel> asks = new ArrayList<>();

        tick.getMarketDepth().get("buy").forEach(d -> bids.add(new DepthLevel(d.getPrice(), d.getQuantity())));

        tick.getMarketDepth().get("sell").forEach(d -> asks.add(new DepthLevel(d.getPrice(), d.getQuantity())));

        return new OrderBookSnapshot(System.currentTimeMillis(), tick.getLastTradedPrice(), bids, asks);
    }

    private double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}