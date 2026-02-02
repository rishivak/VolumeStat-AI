package com.algo.analytics;

import com.algo.model.OptionMeta;
import com.algo.model.OrderBookSnapshot;
import com.algo.utils.Log;

import java.util.HashMap;
import java.util.Map;

public class VolatilityExpansionEngine {
    private static final long WINDOW_MS = 2000;
    private static class Window {
        long time;
        double futStart;
        double optStart;
    }
    private static final Map<Long, Window> windows = new HashMap<>();
    public static void evaluate(OptionMeta opt, OrderBookSnapshot futPrev, OrderBookSnapshot futCurr, OrderBookSnapshot optPrev, OrderBookSnapshot optCurr) {
        long key = opt.optionToken;
        Window w = windows.get(key);
        if (w == null) {
            w = new Window();
            w.time = optCurr.timestamp;
            w.futStart = futPrev.ltp;
            w.optStart = optPrev.ltp;
            windows.put(key, w);
            return;
        }
        long age = optCurr.timestamp - w.time;
        if (age < WINDOW_MS) return;
        double futPct = Math.abs(futCurr.ltp - w.futStart) / w.futStart * 100.0;
        double optPct = Math.abs(optCurr.ltp - w.optStart) / w.optStart * 100.0;
        if (optPct > futPct * 3 && optPct > 0.5) {
            Log.info("💥 VOL EXPANSION | opt=" + opt.optionToken + " FUT%=" + round(futPct) + " OPT%=" + round(optPct));
        }
        windows.remove(key);
    }
    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
