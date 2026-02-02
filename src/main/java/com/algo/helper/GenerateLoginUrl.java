package com.algo.helper;

import com.algo.config.AppConfig;
import com.algo.utils.Log;
import com.zerodhatech.kiteconnect.KiteConnect;

public class GenerateLoginUrl {
    public static void main(String[] args) {

        String apiKey = AppConfig.get("kite.api.key");
        KiteConnect kite = new KiteConnect(apiKey);
        Log.info("Open this URL in browser:");
        Log.info(kite.getLoginURL());
    }
}
