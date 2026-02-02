package com.algo.model;

import java.util.List;

public class OrderBookSnapshot {

    public long timestamp;
    public double ltp;
    public long volume;                 // 🔥 ADD THIS
    public List<DepthLevel> bids;
    public List<DepthLevel> asks;

    public OrderBookSnapshot(long timestamp,
                             double ltp,
                             long volume,
                             List<DepthLevel> bids,
                             List<DepthLevel> asks) {

        this.timestamp = timestamp;
        this.ltp = ltp;
        this.volume = volume;
        this.bids = bids;
        this.asks = asks;
    }
    // ✅ BACKWARD-COMPAT CONSTRUCTOR (VERY IMPORTANT)
    public OrderBookSnapshot(long timestamp,
                             double ltp,
                             List<DepthLevel> bids,
                             List<DepthLevel> asks) {

        this(timestamp, ltp, 0L, bids, asks);
    }
}
