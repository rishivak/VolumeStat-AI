package com.algo.state;

import java.util.concurrent.ConcurrentHashMap;

public class LastPriceCache {

    private static final ConcurrentHashMap<Long, Double> futPrices = new ConcurrentHashMap<>();

    public static void update(long token, double ltp) {
        futPrices.put(token, ltp);
    }

    public static double getFutLtp(long token) {
        return futPrices.getOrDefault(token, 0.0);
    }
}
