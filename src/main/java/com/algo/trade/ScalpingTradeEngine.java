package com.algo.trade;

import com.algo.config.AppConfig;
import com.algo.config.InstrumentStore;
import com.algo.enums.SignalDirection;
import com.algo.utils.Log;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import java.util.HashMap;
import java.util.Map;

public class ScalpingTradeEngine {
    private static final long NIFTY_FUT = AppConfig.getLong("nifty.fut.token");
    private static final Map<Long, TradeIntent> activeTrades = new HashMap<>();

    public static void tryEnter(long futToken, SignalDirection dir, double ltp, int confidence) {
        if (activeTrades.containsKey(futToken)) return;
        if (confidence < 65) return;
        double sl = dir == SignalDirection.UP ? ltp - stopLossPoints(futToken) : ltp + stopLossPoints(futToken);
        //------------ Actual trade
//        int qty = InstrumentStore.getLotSize(futToken);
//        try {
//            // 🔥 ENTRY
//            String entryOrderId = ZerodhaOrderExecutor.placeMarketEntry(futToken, dir, qty);
//            // 🔥 STOP LOSS
//            String slOrderId = ZerodhaOrderExecutor.placeSLMarket(futToken, dir, qty, sl);
//            TradeIntent trade = new TradeIntent(futToken, dir, ltp, sl, confidence);
//            trade.entryOrderId = entryOrderId;
//            trade.slOrderId = slOrderId;
//            activeTrades.put(futToken, trade);
//            Log.info("🟢 LIVE TRADE | fut=" + futToken + " entryId=" + entryOrderId + " slId=" + slOrderId + " entry=" + ltp + " SL=" + sl + " conf=" + confidence);
//        } catch (Exception | KiteException e) {
//            Log.error("❌ ORDER FAILED | fut=" + futToken + " " + e.getMessage());
//        }
        //-----------------------------
        TradeIntent trade = new TradeIntent(futToken, dir, ltp, sl, confidence);
        activeTrades.put(futToken, trade);
        Log.info("🟢 TRADE ENTRY | fut=" + futToken + " dir=" + dir + " entry=" + ltp + " SL=" + sl + " conf=" + confidence);
        // 🔥 later → Zerodha order placement

    }
    public static void onTick(long futToken, double ltp) {
        TradeIntent t = activeTrades.get(futToken);
        if (t == null) return;
        //--------- Actual SL----------
//        boolean stopHit = t.direction == SignalDirection.UP && ltp <= t.stopLoss || t.direction == SignalDirection.DOWN && ltp >= t.stopLoss;
//        if (!stopHit) return;
//        try {
//            ZerodhaOrderExecutor.exitMarket(futToken, t.direction, InstrumentStore.getLotSize(futToken));
//            Log.info("🔴 FORCE EXIT | fut=" + futToken + " exit=" + ltp);
//        } catch (Exception | KiteException e) {
//            Log.error("❌ EXIT FAILED | fut=" + futToken + " " + e.getMessage());
//        }
//        activeTrades.remove(futToken);
        //-------------
        if (t.direction == SignalDirection.UP && ltp <= t.stopLoss || t.direction == SignalDirection.DOWN && ltp >= t.stopLoss) {
            Log.info("🔴 STOP HIT | fut=" + futToken + " exit=" + ltp);
            activeTrades.remove(futToken);
        }
    }
    private static double stopLossPoints(long futToken) {
        // simple heuristic
        return futToken == NIFTY_FUT ? 10 : 20;
    }
}
