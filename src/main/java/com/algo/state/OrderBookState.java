package com.algo.state;

import com.algo.model.OrderBookSnapshot;

import java.util.concurrent.ConcurrentHashMap;

public class OrderBookState {

    private static final ConcurrentHashMap<Long, OrderBookSnapshot> CURR =
            new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, OrderBookSnapshot> PREV =
            new ConcurrentHashMap<>();

    public static OrderBookSnapshot getCurr(long token) {
        return CURR.get(token);
    }

    public static OrderBookSnapshot getPrev(long token) {
        return PREV.get(token);
    }

    public static void update(long token, OrderBookSnapshot snapshot) {
        OrderBookSnapshot old = CURR.put(token, snapshot);
        if (old != null) {
            PREV.put(token, old);
        }
    }
}
