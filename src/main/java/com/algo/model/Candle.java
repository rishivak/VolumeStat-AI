package com.algo.model;

public class Candle {
    public final long startTime;
    public double open;
    public double high;
    public double low;
    public double close;
    public long volume;

    public Candle(long startTime, double price) {
        this.startTime = startTime;
        this.open = price;
        this.high = price;
        this.low = price;
        this.close = price;
        this.volume = 0;
    }
}