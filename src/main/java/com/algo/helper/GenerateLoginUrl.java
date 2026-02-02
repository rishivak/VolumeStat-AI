package com.algo.helper;

import com.algo.utils.Log;
import com.zerodhatech.kiteconnect.KiteConnect;

public class GenerateLoginUrl {
    public static void main(String[] args) {

        String apiKey = "dtvf5j4drx9k1ub9";

        KiteConnect kite = new KiteConnect(apiKey);
        Log.info("Open this URL in browser:");
        Log.info(kite.getLoginURL());
    }
}
