package com.algo.app;

import com.algo.config.AppConfig;
import com.algo.engine.UnderlyingContext;
import com.algo.instrument.InstrumentLoader;
import com.algo.options.OptionSelector;
import com.algo.ws.ZerodhaOrderBookWS;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

import java.util.ArrayList;
import java.util.List;

public class AlgoMain {

    public static void main(String[] args) throws KiteException {

        String apiKey = AppConfig.get("kite.api.key");
        String accessToken = AppConfig.get("kite.access.token");

        KiteConnect kite = new KiteConnect(apiKey);
        kite.setAccessToken(accessToken);

        var instruments = InstrumentLoader.loadNFO();

        long niftyFut = AppConfig.getLong("nifty.fut.token");
        long bankNiftyFut = AppConfig.getLong("banknifty.fut.token");

        double niftyPrice = AppConfig.getDouble("nifty.initial.price");
        double bankPrice = AppConfig.getDouble("banknifty.initial.price");

        int niftyStep = AppConfig.getInt("nifty.strike.step");
        int bankStep = AppConfig.getInt("banknifty.strike.step");

        var niftyOpts = OptionSelector.selectATMPlusMinusOne(
                "NIFTY", niftyPrice, instruments, niftyFut, niftyStep
        );

        var bankOpts = OptionSelector.selectATMPlusMinusOne(
                "BANKNIFTY", bankPrice, instruments, bankNiftyFut, bankStep
        );

        UnderlyingContext niftyCtx =
                new UnderlyingContext("NIFTY", niftyFut, niftyStep, niftyOpts);

        UnderlyingContext bankCtx =
                new UnderlyingContext("BANKNIFTY", bankNiftyFut, bankStep, bankOpts);

        List<UnderlyingContext> contexts = List.of(niftyCtx, bankCtx);

        List<Long> tokens = new ArrayList<>();
        tokens.add(niftyFut);
        tokens.add(bankNiftyFut);
        niftyOpts.forEach(o -> tokens.add(o.optionToken));
        bankOpts.forEach(o -> tokens.add(o.optionToken));

        new ZerodhaOrderBookWS(contexts)
                .start(kite, tokens);
    }
}
