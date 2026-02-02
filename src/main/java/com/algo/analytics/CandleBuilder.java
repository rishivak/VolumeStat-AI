package com.algo.analytics;

import com.algo.model.Candle;
import com.algo.model.OrderBookSnapshot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class CandleBuilder {
    private static final long CANDLE_MS = 30_000; // 30 sec
    private static final int MAX_CANDLES = 6;     // last 3 minutes
    private static final Map<Long, Deque<Candle>> candles = new HashMap<>();

    public static void onTick(long token, OrderBookSnapshot curr) {
        long bucket = curr.timestamp / CANDLE_MS * CANDLE_MS;
        Deque<Candle> deque = candles.computeIfAbsent(token, t -> new ArrayDeque<>());
        Candle c = deque.peekLast();
        if (c == null || c.startTime != bucket) {
            c = new Candle(bucket, curr.ltp);
            deque.addLast(c);
            if (deque.size() > MAX_CANDLES) deque.removeFirst();
        }
        c.high = Math.max(c.high, curr.ltp);
        c.low = Math.min(c.low, curr.ltp);
        c.close = curr.ltp;
        c.volume += curr.volume;
    }

    public static Deque<Candle> getCandles(long token) {
        return candles.get(token);
    }
}
