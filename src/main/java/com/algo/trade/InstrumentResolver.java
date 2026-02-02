package com.algo.trade;

import com.algo.config.InstrumentStore;

public class InstrumentResolver {

    public static String getTradingSymbol(long futToken) {
        return InstrumentStore.getTradingSymbol(futToken);
    }
}
