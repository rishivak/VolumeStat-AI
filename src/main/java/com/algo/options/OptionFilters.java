package com.algo.options;

import com.algo.model.DepthLevel;
import com.algo.model.OrderBookSnapshot;

public class OptionFilters {

    public static boolean liquidityOk(OrderBookSnapshot s, int lotSize) {
        long bestBid = s.bids.get(0).quantity;
        long bestAsk = s.asks.get(0).quantity;
        long totalBid = s.bids.stream().mapToLong(b -> b.quantity).sum();
        long totalAsk = s.asks.stream().mapToLong(a -> a.quantity).sum();
        return (bestBid + bestAsk) >= (2L * lotSize) && (totalBid + totalAsk) >= (10L * lotSize);
    }
    public static boolean spreadOk(OrderBookSnapshot s) {
        double bid = s.bids.get(0).price;
        double ask = s.asks.get(0).price;
        double spreadPct = (ask - bid) / s.ltp;
        return spreadPct <= 0.002; // 0.2%
    }
    public static boolean priceOk(OrderBookSnapshot s) {
        return s.ltp >= 30.0;
    }
}
