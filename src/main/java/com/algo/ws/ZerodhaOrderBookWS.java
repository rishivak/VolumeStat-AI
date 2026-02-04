package com.algo.ws;

import com.algo.analytics.*;
import com.algo.engine.UnderlyingContext;
import com.algo.model.DepthLevel;
import com.algo.model.OptionMeta;
import com.algo.model.OrderBookSnapshot;
import com.algo.state.LastPriceCache;
import com.algo.state.OrderBookState;
import com.algo.trade.ScalpingTradeEngine;
import com.algo.utils.Log;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.*;

import java.util.*;

/**
 * Zerodha WebSocket handler for FUT + OPTION orderbook tracking
 */
public class ZerodhaOrderBookWS {

    private KiteTicker ticker;

    // futToken -> context
    private final Map<Long, UnderlyingContext> futContexts = new HashMap<>();

    // optionToken -> context
    private final Map<Long, UnderlyingContext> optionContexts = new HashMap<>();

    // optionToken -> option meta
    private final Map<Long, OptionMeta> optionMetaMap = new HashMap<>();

    // =====================================================
    // 🧠 CONSTRUCTOR
    // =====================================================
    public ZerodhaOrderBookWS(List<UnderlyingContext> contexts) {

        for (UnderlyingContext ctx : contexts) {
            futContexts.put(ctx.futToken, ctx);

            ctx.options.forEach((token, opt) -> {
                optionContexts.put(token, ctx);
                optionMetaMap.put(token, opt);
            });
        }
    }

    // =====================================================
    // 🚀 START WS
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
                Log.error(e.message);
            }

            @Override
            public void onError(String error) {
                Log.error(error);
            }
        });

        ticker.setOnTickerArrivalListener(ticks -> {
            for (Tick tick : ticks) {
                handleTick(tick);
            }
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
        if (prev == null){
            Log.info("Getting previous order book state for token : "+token);
            return;
        }

        // =====================================================
        // 🔹 FUTURES HANDLING
        // =====================================================
        CandleBuilder.onTick(token, curr);
//        MicroBreakoutEngine.evaluate(token, CandleBuilder.getCandles(token));
        UnderlyingContext futCtx = futContexts.get(token);
        if (futCtx != null) {
            LastPriceCache.update(token, curr.ltp);
            double pressure = OrderBookCalculator.weightedPressure(curr.bids, curr.asks);
            long aggression = OrderBookCalculator.aggression(prev, curr);

            // --- ladder diagnostic (5 depth)
            long bid5 = curr.bids.stream().limit(5).mapToLong(b -> b.quantity).sum();
            long ask5 = curr.asks.stream().limit(5).mapToLong(a -> a.quantity).sum();

            double ladderP = bid5 / (double) (bid5 + ask5 + 1);
            // 🔥 MOMENTUM IGNITION
            MomentumIgnitionEngine.evaluate(futCtx.name, token, prev, curr, ladderP, pressure);
            //  heartbeat every tick (safe)
//            Log.info(futCtx.name + " FUT | LTP=" + curr.ltp + " ladderP=" + round(ladderP)+" Pressure="+ pressure + " agg=" + aggression);
            // 🔥 EXACT EXIT MANAGEMENT POINT
            ScalpingTradeEngine.onTick(token, curr.ltp);
            return;
        }

        // =====================================================
        // 🔹 OPTION HANDLING
        // =====================================================
        UnderlyingContext optCtx = optionContexts.get(token);
        if (optCtx == null) return;

        OptionMeta opt = optionMetaMap.get(token);
        if (opt == null) return;
//        OptionConfidenceEngine.onOptionTick(opt,prev, curr);

        OrderBookSnapshot futCurr = OrderBookState.getCurr(optCtx.futToken);
        OrderBookSnapshot futPrev = OrderBookState.getPrev(optCtx.futToken);

        if (futCurr == null || futPrev == null) return;
//        VolatilityExpansionEngine.evaluate(opt, futPrev, futCurr, prev, curr);
        // 🔥 OPTION PREMIUM FLOW
        OptionPremiumFlowEngine.evaluate(opt, prev, curr);
        OptionSignalEngine.evaluateOption(opt, futPrev, futCurr, prev, curr);
    }

    // =====================================================
    // 🧱 BUILD SNAPSHOT
    // =====================================================
    private OrderBookSnapshot buildSnapshot(Tick tick) {

        List<DepthLevel> bids = new ArrayList<>();
        List<DepthLevel> asks = new ArrayList<>();

        tick.getMarketDepth().get("buy").forEach(d -> bids.add(new DepthLevel(d.getPrice(), d.getQuantity())));

        tick.getMarketDepth().get("sell").forEach(d -> asks.add(new DepthLevel(d.getPrice(), d.getQuantity())));

        return new OrderBookSnapshot(System.currentTimeMillis(), tick.getLastTradedPrice(), tick.getVolumeTradedToday(), bids, asks);
    }

    private double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}