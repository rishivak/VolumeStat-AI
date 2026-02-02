package com.algo.instrument;

import com.algo.model.InstrumentInfo;
import com.algo.utils.Log;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class InstrumentLoader {

    public static List<InstrumentInfo> loadNFO() {

        List<InstrumentInfo> list = new ArrayList<>();

        InputStream is = InstrumentLoader.class
                .getClassLoader()
                .getResourceAsStream("instruments_nfo.csv");

        if (is == null) {
            throw new RuntimeException(
                    "❌ instruments_nfo.csv NOT found in src/main/resources"
            );
        }

        int total = 0, opt = 0, symbolMatch = 0, typeMatch = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            br.readLine(); // header
            String line;

            while ((line = br.readLine()) != null) {
                total++;

                String[] c = line.split(",", -1);
                if (c.length < 12) continue;

                String segment = c[10].trim();
                if (!segment.equals("NFO-OPT")) continue;
                opt++;

                String tradingSymbol = c[2].trim();

                boolean isNifty =
                        tradingSymbol.startsWith("NIFTY")
                                && !tradingSymbol.startsWith("NIFTYNXT");

                boolean isBankNifty =
                        tradingSymbol.startsWith("BANKNIFTY");

                if (!isNifty && !isBankNifty) continue;
                symbolMatch++;

                String type = c[9].trim();
                if (!type.equals("CE") && !type.equals("PE")) continue;
                typeMatch++;

                String strikeStr = c[6].trim();
                String expiryStr = c[5].trim();
                if (strikeStr.isEmpty() || expiryStr.isEmpty())
                    continue;

                list.add(new InstrumentInfo(
                        Long.parseLong(c[0].trim()),     // token
                        isNifty ? "NIFTY" : "BANKNIFTY",
                        tradingSymbol,
                        (int) Double.parseDouble(strikeStr),
                        type,
                        LocalDate.parse(expiryStr),
                        Integer.parseInt(c[8].trim())    // lot size
                ));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Log.info("CSV rows            : " + total);
        Log.info("NFO-OPT rows        : " + opt);
        Log.info("Symbol match        : " + symbolMatch);
        Log.info("CE/PE rows          : " + typeMatch);
        Log.info("✅ Loaded NFO options: " + list.size());

        if (list.isEmpty()) {
            throw new RuntimeException(
                    "❌ Zero options loaded – tradingsymbol filter failed"
            );
        }

        return list;
    }
}
