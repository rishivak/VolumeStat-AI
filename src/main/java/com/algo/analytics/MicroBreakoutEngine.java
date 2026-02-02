package com.algo.analytics;

import com.algo.model.Candle;
import com.algo.utils.Log;

import java.util.Deque;
public class MicroBreakoutEngine {
    public static void evaluate(long token, Deque<Candle> candles) {
        if (candles == null || candles.size() < 4) return;
        Candle last = candles.peekLast();
        double high = Double.MIN_VALUE;
        double low = Double.MAX_VALUE;
        long avgVol = 0;
        int count = 0;
        for (Candle c : candles) {
            if (c != last) {
                high = Math.max(high, c.high);
                low = Math.min(low, c.low);
                avgVol += c.volume;
                count++;
            }
        }
        avgVol /= Math.max(1, count);
        // 🔥 BREAKOUT UP
        if (last.close > high && last.volume > avgVol * 1.5) {
            Log.info("⚡ MICRO BREAKOUT UP | token=" + token + " price=" + last.close + " vol=" + last.volume);
        }
        // 🔻 BREAKOUT DOWN
        if (last.close < low && last.volume > avgVol * 1.5) {
            Log.info("⚡ MICRO BREAKOUT DOWN | token=" + token + " price=" + last.close + " vol=" + last.volume);
        }
    }
}
