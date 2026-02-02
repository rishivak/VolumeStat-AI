package com.algo.model;

import java.util.List;

public class OrderBookSnapshot {
    public long timestamp;
    public double ltp;
    public List<DepthLevel> bids;
    public List<DepthLevel> asks;

    public OrderBookSnapshot(long timestamp, double ltp,
                             List<DepthLevel> bids,
                             List<DepthLevel> asks) {
        this.timestamp = timestamp;
        this.ltp = ltp;
        this.bids = bids;
        this.asks = asks;
    }
}
