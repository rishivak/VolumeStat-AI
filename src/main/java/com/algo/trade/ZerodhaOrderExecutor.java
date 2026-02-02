package com.algo.trade;

import com.algo.enums.SignalDirection;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.OrderParams;

import java.io.IOException;

public class ZerodhaOrderExecutor {

    private static KiteConnect kite;

    public static void init(KiteConnect kiteConnect) {
        kite = kiteConnect;
    }

    // ================= ENTRY =================
    public static String placeMarketEntry(long futToken, SignalDirection dir, int qty) throws KiteException, IOException {

        OrderParams order = new OrderParams();
        order.exchange = "NFO";
        order.tradingsymbol = InstrumentResolver.getTradingSymbol(futToken);
        order.transactionType = dir == SignalDirection.UP ? "BUY" : "SELL";
        order.quantity = qty;
        order.orderType = "MARKET";
        order.product = "MIS";
        order.validity = "DAY";

        return kite.placeOrder(order, "regular").orderId;
    }

    // ================= STOP LOSS =================
    public static String placeSLMarket(long futToken, SignalDirection dir, int qty, double triggerPrice) throws KiteException, IOException {

        OrderParams sl = new OrderParams();
        sl.exchange = "NFO";
        sl.tradingsymbol = InstrumentResolver.getTradingSymbol(futToken);
        sl.transactionType = dir == SignalDirection.UP ? "SELL" : "BUY";
        sl.quantity = qty;
        sl.orderType = "SL-M";
        sl.triggerPrice = triggerPrice;
        sl.product = "MIS";
        sl.validity = "DAY";

        return kite.placeOrder(sl, "regular").orderId;
    }

    // ================= EXIT =================
    public static void exitMarket(long futToken, SignalDirection dir, int qty) throws KiteException, IOException {

        OrderParams exit = new OrderParams();
        exit.exchange = "NFO";
        exit.tradingsymbol = InstrumentResolver.getTradingSymbol(futToken);
        exit.transactionType = dir == SignalDirection.UP ? "SELL" : "BUY";
        exit.quantity = qty;
        exit.orderType = "MARKET";
        exit.product = "MIS";

        kite.placeOrder(exit, "regular");
    }
}
