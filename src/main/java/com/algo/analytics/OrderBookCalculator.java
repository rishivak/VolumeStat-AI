package com.algo.analytics;

import com.algo.model.DepthLevel;
import com.algo.model.OrderBookSnapshot;

import java.util.List;

public class OrderBookCalculator {

    private static final int[] WEIGHTS = {5, 4, 3, 2, 1};

    // 🔹 Weighted pressure (ladder bias)
    public static double weightedPressure(List<DepthLevel> bids, List<DepthLevel> asks) {
        long bidScore = 0;
        long askScore = 0;
        for (int i = 0; i < 5; i++) {
            bidScore += bids.get(i).quantity * WEIGHTS[i];
            askScore += asks.get(i).quantity * WEIGHTS[i];
        }
        return (double) bidScore / (bidScore + askScore);
    }
    // 🔹 Total quantity helper
    private static long totalQty(List<DepthLevel> levels) {
        return levels.stream().mapToLong(l -> l.quantity).sum();
    }
    // 🔹 Aggression: who is hitting the ladder
    public static long aggression(OrderBookSnapshot prev, OrderBookSnapshot curr) {
        long askHit = totalQty(prev.asks) - totalQty(curr.asks);
        long bidHit = totalQty(prev.bids) - totalQty(curr.bids);
        return askHit - bidHit; // +ve = buyers aggressive
    }
    // 🔹 Price velocity (momentum confirmation)
    public static double priceVelocity(OrderBookSnapshot prev, OrderBookSnapshot curr) {
        long dt = curr.timestamp - prev.timestamp;
        if (dt <= 0) return 0;
        return (curr.ltp - prev.ltp) / dt;
    }
    // 🔹 Absorption detection
    public static boolean absorption(OrderBookSnapshot prev, OrderBookSnapshot curr) {
        long prevAsk = totalQty(prev.asks);
        long currAsk = totalQty(curr.asks);
        boolean askNotReducing = Math.abs(currAsk - prevAsk) < 100;
        boolean priceNotFalling = curr.ltp >= prev.ltp;
        return askNotReducing && priceNotFalling;
    }
}
