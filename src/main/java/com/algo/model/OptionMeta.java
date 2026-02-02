package com.algo.model;

public class OptionMeta {

    public long optionToken;
    public long futToken;
    public String type;    // CE / PE
    public int strike;
    public int lotSize;

    public OptionMeta(long optionToken,
                      long futToken,
                      String type,
                      int strike,
                      int lotSize) {

        this.optionToken = optionToken;
        this.futToken = futToken;
        this.type = type;
        this.strike = strike;
        this.lotSize = lotSize;
    }
}
