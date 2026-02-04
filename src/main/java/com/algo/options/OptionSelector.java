package com.algo.options;

import com.algo.model.InstrumentInfo;
import com.algo.model.OptionMeta;
import com.algo.utils.Log;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class OptionSelector {

    public static List<OptionMeta> selectATMPlusMinusOne(String underlying,               // NIFTY / BANKNIFTY
                                                         double futPrice, List<InstrumentInfo> instruments, long futToken, int step) {
        // 1️⃣ Filter only this underlying
        List<InstrumentInfo> filtered = instruments.stream().filter(i -> i.name.equals(underlying)).collect(Collectors.toList());
        if (filtered.isEmpty()) {
            Log.error(underlying + " | No instruments after underlying filter");
            return List.of();
        }
        // 2️⃣ Find nearest expiry FOR THIS UNDERLYING
        LocalDate nearestExpiry = filtered.stream().map(i -> i.expiry).min(LocalDate::compareTo).orElseThrow();
        // 3️⃣ ATM strikes
        int atm = (int) (Math.round(futPrice / step) * step);
        Set<Integer> strikes = Set.of(atm - step, atm, atm + step);
        // 4️⃣ Select options
        List<OptionMeta> result = filtered.stream().filter(i -> i.expiry.equals(nearestExpiry)).filter(i -> strikes.contains(i.strike)).map(i -> new OptionMeta(i.token, futToken, i.type, i.strike, i.lotSize,i.tradingSymbol)).collect(Collectors.toList());

        Log.info(underlying + " | Selected " + result.size() + " options | expiry=" + nearestExpiry + " ATM=" + atm);

        return result;
    }
}
