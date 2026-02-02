package com.algo.helper;

import com.algo.utils.Log;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.User;

public class GenerateAccessToken {

    public static void main(String[] args) throws Exception, KiteException {

        String apiKey = "dtvf5j4drx9k1ub9";
        String apiSecret = "tgg8ntz4lj2aocbsz8st1njy6nxdl21u";
        String requestToken = "C2cVteavulOY1UNc1ey5RPjKeLncNH3n";

        KiteConnect kite = new KiteConnect(apiKey);
        User user = kite.generateSession(requestToken, apiSecret);

        Log.info("ACCESS TOKEN:");
        Log.info(user.accessToken);
    }
}
