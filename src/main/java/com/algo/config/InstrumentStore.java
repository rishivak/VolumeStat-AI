package com.algo.config;

import com.algo.model.InstrumentInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstrumentStore {

    private static final Map<Long, String> tokenToSymbol = new HashMap<>();
    private static final Map<Long, Integer> tokenToLotSize = new HashMap<>();

    // call this ONCE during startup
    public static void init(List<InstrumentInfo> instruments) {
        for (InstrumentInfo i : instruments) {
            tokenToSymbol.put(i.token, i.tradingSymbol);
            tokenToLotSize.put(i.token, i.lotSize);
        }
    }

    public static String getTradingSymbol(long token) {
        String symbol = tokenToSymbol.get(token);
        if (symbol == null) {
            throw new RuntimeException("No tradingSymbol for token " + token);
        }
        return symbol;
    }

    public static int getLotSize(long token) {
        Integer lot = tokenToLotSize.get(token);
        if (lot == null) {
            throw new RuntimeException("No lotSize for token " + token);
        }
        return lot;
    }
}
