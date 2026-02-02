package com.algo.helper;

import com.algo.config.AppConfig;
import com.algo.utils.Log;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.User;

public class GenerateAccessToken {

    public static void main(String[] args) throws Exception, KiteException {


        String apiKey = AppConfig.get("kite.api.key");
        String apiSecret = AppConfig.get("kite.api.secret");
        String requestToken = AppConfig.get("kite.request.token");

        KiteConnect kite = new KiteConnect(apiKey);
        User user = kite.generateSession(requestToken, apiSecret);

        Log.info("ACCESS TOKEN:");
        Log.info(user.accessToken);
    }
}
