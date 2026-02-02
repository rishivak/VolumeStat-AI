package com.algo.trade;
import com.algo.enums.SignalDirection;

public class TradeIntent {

    public long futToken;
    public SignalDirection direction;
    public double entryPrice;
    public double stopLoss;
    public int confidence;


    public String entryOrderId;
    public String slOrderId;

    public TradeIntent(long futToken,
                       SignalDirection direction,
                       double entryPrice,
                       double stopLoss,
                       int confidence) {
        this.futToken = futToken;
        this.direction = direction;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.confidence = confidence;
    }
}
