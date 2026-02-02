package com.algo.analytics;

import com.algo.model.DepthLevel;
import com.algo.model.OptionMeta;
import com.algo.model.OrderBookSnapshot;
import com.algo.utils.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptionConfidenceEngine {
    private static final long WINDOW_MS = 5000;
    private static final Map<Long, Window> windows = new HashMap<>();
    private static class Window {
        long startTime;
        double callFlow;
        double putFlow;
        long callBid;
        long callAsk;
        long putBid;
        long putAsk;
    }
    public static void onOptionTick(OptionMeta opt, OrderBookSnapshot prev, OrderBookSnapshot curr) {
        long futToken = opt.futToken;
        Window w = windows.computeIfAbsent(futToken, t -> {
            Window nw = new Window();
            nw.startTime = curr.timestamp;
            return nw;
        });
        double premiumDelta = (curr.ltp - prev.ltp) * opt.lotSize;
        long bid5 = sumQty(curr.bids);
        long ask5 = sumQty(curr.asks);
        if ("CE".equals(opt.type)) {
            w.callFlow += premiumDelta;
            w.callBid += bid5;
            w.callAsk += ask5;
        } else {
            w.putFlow += premiumDelta;
            w.putBid += bid5;
            w.putAsk += ask5;
        }
        if (curr.timestamp - w.startTime < WINDOW_MS) return;
        emitConfidence(futToken, w);
        windows.remove(futToken);
    }
    private static void emitConfidence(long futToken, Window w) {
        int callScore = computeScore(w.callFlow, w.callBid, w.callAsk, w.putFlow, w.putBid, w.putAsk);
        int putScore = computeScore(w.putFlow, w.putBid, w.putAsk, w.callFlow, w.callBid, w.callAsk);
        Log.info("📊 OPTION CONFIDENCE | fut=" + futToken + " CALL=" + callScore + " PUT=" + putScore);
    }
    private static int computeScore(double domFlow, long domBid, long domAsk, double oppFlow, long oppBid, long oppAsk) {
        int score = 0;
        // premium dominance (max 40)
        score += Math.min(40, (int) (Math.abs(domFlow) / (Math.abs(oppFlow) + 1) * 10));
        // ladder dominance (max 40)
        score += Math.min(40, (int) ((double) domBid / (domAsk + 1) * 20));
        // absolute participation (max 20)
        score += Math.min(20, (int) ((domBid + domAsk) / 5000));
        return Math.min(100, score);
    }
    private static long sumQty(List<DepthLevel> levels) {
        return levels.stream().limit(5).mapToLong(d -> d.quantity).sum();
    }
}