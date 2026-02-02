package com.algo.analytics;

import com.algo.model.OrderBookSnapshot;
import com.algo.utils.Log;

public class SignalEngine {

    public static void evaluate(long token,
                                OrderBookSnapshot prev,
                                OrderBookSnapshot curr) {

        double pressure =
                OrderBookCalculator.weightedPressure(curr.bids, curr.asks);

        long aggression =
                OrderBookCalculator.aggression(prev, curr);

        double velocity =
                OrderBookCalculator.priceVelocity(prev, curr);

        boolean absorption =
                OrderBookCalculator.absorption(prev, curr);

        // 🔥 BUY SETUP (ladder breakout)
        if (pressure > 0.65 &&
                aggression > 500 &&
                velocity > 0 &&
                absorption) {

            Log.info(
                    "🔥 BUY SIGNAL | token=" + token +
                            " pressure=" + round(pressure) +
                            " aggression=" + aggression +
                            " velocity=" + round(velocity)
            );
        }

        // 🔻 SELL / EXIT SETUP
        if (pressure < 0.35 &&
                aggression < -500 &&
                velocity < 0) {

            Log.info(
                    "🔻 SELL SIGNAL | token=" + token +
                            " pressure=" + round(pressure) +
                            " aggression=" + aggression +
                            " velocity=" + round(velocity)
            );
        }
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
