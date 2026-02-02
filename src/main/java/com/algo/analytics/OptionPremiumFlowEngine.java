package com.algo.analytics;

import com.algo.enums.SignalDirection;
import com.algo.model.OptionMeta;
import com.algo.model.OrderBookSnapshot;
import com.algo.utils.Log;

import java.util.HashMap;
import java.util.Map;

public class OptionPremiumFlowEngine {

    private static final long WINDOW_MS = 3000;

    private static final Map<Long, Window> windows = new HashMap<>();

    private static class Window {
        long startTime;
        double callStart;
        double putStart;
        double callLast;
        double putLast;
    }
    public static void evaluate(OptionMeta opt, OrderBookSnapshot prev, OrderBookSnapshot curr) {
        long futToken = opt.futToken;
        Window w = windows.get(futToken);
        if (w == null) {
            w = new Window();
            w.startTime = curr.timestamp;
            windows.put(futToken, w);
        }
        double premiumDelta = (curr.ltp - prev.ltp) * opt.lotSize;
        if ("CE".equals(opt.type)) {
            w.callLast += premiumDelta;
        } else {
            w.putLast += premiumDelta;
        }
        long age = curr.timestamp - w.startTime;
        if (age < WINDOW_MS) return;
        detect(futToken, w);
        windows.remove(futToken); // reset
    }
    private static void detect(long futToken, Window w) {
        double callFlow = w.callLast;
        double putFlow = w.putLast;
        if (Math.abs(callFlow) < 1 && Math.abs(putFlow) < 1) return;
        if (callFlow > putFlow * 1.5) {
            int score = optionFlowScore(callFlow, putFlow);
            Log.info("📈 OPTION FLOW BULLISH | fut=" + futToken + " CALL=" + round(callFlow) + " PUT=" + round(putFlow) + " score=" + score);
            SignalCoordinator.onOptionFlow(futToken, SignalDirection.UP, score);
        }
        if (putFlow > callFlow * 1.5) {
            int score = optionFlowScore(putFlow, callFlow);
            Log.info("📉 OPTION FLOW BEARISH | fut=" + futToken + " CALL=" + round(callFlow) + " PUT=" + round(putFlow) + " score=" + score);
            SignalCoordinator.onOptionFlow(futToken, SignalDirection.DOWN, score);
        }
    }
    private static int optionFlowScore(double dominant, double opposite) {
        double ratio = dominant / (Math.abs(opposite) + 1);
        int score = 0;
        // dominance ratio (max 60)
        score += Math.min(60, (int) (ratio * 20));
        // absolute flow size (max 40)
        score += Math.min(40, (int) (Math.abs(dominant) / 500));
        return Math.min(100, score);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}