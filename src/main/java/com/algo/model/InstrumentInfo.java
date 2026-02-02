package com.algo.model;

import java.time.LocalDate;

public class InstrumentInfo {

    public long token;
    public String name;          // NIFTY / BANKNIFTY
    public String tradingSymbol;
    public int strike;
    public String type;          // CE / PE
    public LocalDate expiry;
    public int lotSize;

    public InstrumentInfo(long token,
                          String name,
                          String tradingSymbol,
                          int strike,
                          String type,
                          LocalDate expiry,
                          int lotSize) {
        this.token = token;
        this.name = name;
        this.tradingSymbol = tradingSymbol;
        this.strike = strike;
        this.type = type;
        this.expiry = expiry;
        this.lotSize = lotSize;
    }
}
