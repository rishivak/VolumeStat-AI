package com.algo.analytics;

import com.algo.enums.SignalDirection;
import com.algo.model.OrderBookSnapshot;
import com.algo.utils.Log;

import java.util.HashMap;
import java.util.Map;

public class MomentumIgnitionEngine {

    private static final long WINDOW_MS = 3000;

    private static final Map<Long, Window> windows = new HashMap<>();

    private static class Window {
        long startTime;
        double startPrice;
        double lastPrice;
    }

    public static void evaluate(String name, long token, OrderBookSnapshot prev, OrderBookSnapshot curr, double ladderPressure, double pressure) {
        Window w = windows.get(token);
        if (w == null) {
            w = new Window();
            w.startTime = curr.timestamp;
            w.startPrice = curr.ltp;
            w.lastPrice = curr.ltp;
            windows.put(token, w);
            return;
        }
        w.lastPrice = curr.ltp;
        long age = curr.timestamp - w.startTime;
        if (age < WINDOW_MS) return;
        detect(name, token, w, prev, curr, ladderPressure, pressure);
        windows.remove(token); // reset window after evaluation
    }

    private static void detect(String name, long token, Window w, OrderBookSnapshot prev, OrderBookSnapshot curr, double ladderP, double pressure) {
        double pricePct = (w.lastPrice - w.startPrice) / w.startPrice * 100.0;
        long aggression = OrderBookCalculator.aggression(prev, curr);
        // thresholds (safe defaults)
        boolean up = pricePct > 0.05 && aggression > 500 && ladderP > 0.65;
        boolean down = pricePct < -0.05 && aggression < -500 && ladderP < 0.35;
//        boolean up = pricePct > 0.05 && aggression > 500 && ladderP > 0.65 && pressure > 0.55;
//        boolean down = pricePct < -0.05 && aggression < -500 && ladderP < 0.35 && pressure < 0.45;

        if (up) {
            int score = ignitionScore(pricePct, aggression, ladderP,pressure);
            Log.info("🚀 MOMENTUM IGNITION UP | " + name + " priceΔ=" + round(pricePct) + "% agg=" + aggression + " ladderP=" + round(ladderP) + " score=" + score);
            SignalCoordinator.onIgnition(token, SignalDirection.UP, score);
        }
        if (down) {
            int score = ignitionScore(-pricePct, -aggression, 1 - ladderP,pressure);
            Log.info("🧨 MOMENTUM IGNITION DOWN | " + name + " priceΔ=" + round(pricePct) + "% agg=" + aggression + " ladderP=" + round(ladderP) + " score=" + score);
            SignalCoordinator.onIgnition(token, SignalDirection.DOWN, score);
        }
    }
    private static int ignitionScore(double pricePct, long aggression, double ladderP, double pressure) {
        int score = 0;
        // price acceleration (max 40)
        score += Math.min(40, (int) (pricePct * 400));
        // aggression (max 40)
        score += Math.min(40, (int) (Math.abs(aggression) / 20));
        // ladder pressure (max 20)
        score += Math.min(20, (int) (ladderP * 20));
        // pressure alignment bonus (max +15)
        if (pressure > 0.65 || pressure < 0.35) {
            score += 15;
        }

        return Math.min(100, score);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
