package com.algo.analytics;

import com.algo.model.OptionMeta;
import com.algo.model.OrderBookSnapshot;
import com.algo.options.OptionFilters;
import com.algo.utils.Log;

public class OptionSignalEngine {

    public static void evaluateOption(OptionMeta meta, OrderBookSnapshot futPrev, OrderBookSnapshot futCurr, OrderBookSnapshot optPrev, OrderBookSnapshot optCurr) {
        if (futPrev == null || optPrev == null) return;
        // =====================================================
        // 0️⃣ BASIC SAFETY FILTERS
        // =====================================================
        if (!OptionFilters.liquidityOk(optCurr, meta.lotSize)) return;
        if (!OptionFilters.spreadOk(optCurr)) return;
        if (!OptionFilters.priceOk(optCurr)) return;
        // =====================================================
        // 1️⃣ FUTURE ORDER FLOW (LEADER)
        // =====================================================
        double futPressure = OrderBookCalculator.weightedPressure(futCurr.bids, futCurr.asks);
        long futAggression = OrderBookCalculator.aggression(futPrev, futCurr);
        // =====================================================
        // 2️⃣ OPTION ORDER FLOW (FOLLOWER)
        // =====================================================
        double optPressure = OrderBookCalculator.weightedPressure(optCurr.bids, optCurr.asks);
        long optAggression = OrderBookCalculator.aggression(optPrev, optCurr);
        double velocity = OrderBookCalculator.priceVelocity(optPrev, optCurr);
        // =====================================================
        // 3️⃣ EVENT-LEVEL SIGNAL (STRONG MOVE)
        // =====================================================
        // 🟢 CALL BUY (Momentum)
        if ("CE".equals(meta.type) && futPressure > 0.65 && futAggression > 400 && optPressure > 0.55 && optAggression > 150 && velocity > 0) {
            Log.info("🟢 BUY CALL | strike=" + meta.strike + " futP=" + round(futPressure) + " futAgg=" + futAggression + " optAgg=" + optAggression);
        }
        // 🔴 PUT BUY (Momentum)
        if ("PE".equals(meta.type) && futPressure < 0.35 && futAggression < -400 && optPressure < 0.45 && optAggression < -150 && velocity < 0) {
            Log.info("🔴 BUY PUT | strike=" + meta.strike + " futP=" + round(futPressure) + " futAgg=" + futAggression + " optAgg=" + optAggression);
        }
        // =====================================================
        // 4️⃣ 🔁 FREQUENT LADDER-BASED MICRO SIGNAL (PART 3)
        // =====================================================
        long futBid5 = futCurr.bids.stream().limit(5).mapToLong(b -> b.quantity).sum();
        long futAsk5 = futCurr.asks.stream().limit(5).mapToLong(a -> a.quantity).sum();
        double ladderPressure = futBid5 / (double) (futBid5 + futAsk5 + 1);
        // ⚡ SCALP CALL (ladder imbalance)
//        if ("CE".equals(meta.type) && ladderPressure > 0.58 && velocity > 0) {
//            Log.info("⚡ SCALP CALL | strike=" + meta.strike + " ladderP=" + round(ladderPressure) + " vel=" + round(velocity));
//        }
//        // ⚡ SCALP PUT (ladder imbalance)
//        if ("PE".equals(meta.type) && ladderPressure < 0.42 && velocity < 0) {
//            Log.info("⚡ SCALP PUT | strike=" + meta.strike + " ladderP=" + round(ladderPressure) + " vel=" + round(velocity));
//        }
    }
    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
