package com.algo.analytics;

import com.algo.model.OrderBookSnapshot;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AdvancedOptionFlowEngine {

    private static final Map<Long, State> STATES = new ConcurrentHashMap<>();

    private AdvancedOptionFlowEngine() {}

    public static Result evaluate(long token, String symbol, OrderBookSnapshot prev, OrderBookSnapshot curr) {
        if (prev == null || curr == null) return Result.empty();

        State st = STATES.computeIfAbsent(token, k -> new State());

        double midPrev = mid(prev);
        double midCurr = mid(curr);
        int dir = Double.compare(midCurr, midPrev);
        if (dir == 0) dir = st.lastDir;

        double ltpPrev = safeLtp(prev, midPrev);
        double ltpCurr = safeLtp(curr, midCurr);

        long volPrev = safeVolume(prev);
        long volCurr = safeVolume(curr);
        long dVol = Math.max(0, volCurr - volPrev);

        long oiPrev = safeOi(prev);
        long oiCurr = safeOi(curr);
        long dOi = (oiPrev == 0 || oiCurr == 0) ? 0 : (oiCurr - oiPrev);

        long lastQty = safeLastQty(curr);

        // 1) Trade-based delta when we have a volume increment; else proxy via depth change
        long tradeDelta = 0;
        boolean usedTrade = false;
        if (dVol > 0) {
            long qty = lastQty > 0 ? lastQty : dVol;
            int tradeSide = inferTradeSide(ltpPrev, ltpCurr, midPrev, midCurr, dir);
            tradeDelta = tradeSide * qty;
            usedTrade = true;
        } else {
            tradeDelta = depthProxyDelta(prev, curr, 3, dir);
        }

        st.cvd += tradeDelta;

        // 2) Microstructure features from depth
        double spread = spread(curr);
        double micro = microPrice(curr);
        double microShift = (st.lastMicro == 0.0) ? 0.0 : (micro - st.lastMicro);

        long topNAbsChg = topNAbsQtyChange(prev, curr, 5);
        double imbalance = imbalance(curr, 5);

        // 3) Rolling baselines (EWMA mean/var => z-score)
        st.absDeltaStats.update(Math.abs(tradeDelta));
        st.dVolStats.update(dVol);
        st.topNStats.update(topNAbsChg);
        st.spreadStats.update(spread);
        st.imbStats.update(imbalance);
        if (dOi != 0) st.dOiStats.update(Math.abs(dOi));

        double zAbsDelta = st.absDeltaStats.z(Math.abs(tradeDelta));
        double zDVol = st.dVolStats.z(dVol);
        double zTopN = st.topNStats.z(topNAbsChg);
        double zSpread = st.spreadStats.z(spread);
        double zImb = st.imbStats.z(imbalance);
        double zDOi = (dOi == 0) ? 0.0 : st.dOiStats.z(Math.abs(dOi));

        // 4) Regime filters / score
        boolean spreadOk = spread > 0 && zSpread < 2.5; // avoid garbage fills during sudden widening
        boolean pressureAligned = (tradeDelta >= 0 && imbalance > 0.55) || (tradeDelta <= 0 && imbalance < 0.45);

        double score =
                1.10 * clamp(zAbsDelta) +
                0.90 * clamp(zDVol) +
                0.75 * clamp(zTopN) +
                0.50 * clamp(zDOi) +
                0.60 * clamp(Math.abs(zImb)) -
                0.40 * clamp(Math.max(0.0, zSpread));

        boolean surge = spreadOk
                && pressureAligned
                && zAbsDelta >= 2.8
                && zDVol >= 2.2
                && (zTopN >= 1.8 || Math.abs(microShift) > 0)
                && score >= 5.0;

        Result r = new Result();
        r.token = token;
        r.symbol = symbol;
        r.usedTradeDelta = usedTrade;
        r.delta = tradeDelta;
        r.cvd = st.cvd;
        r.dVol = dVol;
        r.dOi = dOi;
        r.spread = spread;
        r.imbalance = imbalance;
        r.micro = micro;
        r.microShift = microShift;
        r.score = score;
        r.zAbsDelta = zAbsDelta;
        r.zDVol = zDVol;
        r.zTopN = zTopN;
        r.zSpread = zSpread;
        r.zImb = zImb;
        r.zDOi = zDOi;
        r.surge = surge;

        st.lastDir = dir;
        st.lastMicro = micro;

        return r;
    }

    private static int inferTradeSide(double ltpPrev, double ltpCurr, double midPrev, double midCurr, int dir) {
        int byLtp = Double.compare(ltpCurr, ltpPrev);
        if (byLtp != 0) return byLtp;
        int byMid = Double.compare(midCurr, midPrev);
        if (byMid != 0) return byMid;
        return dir == 0 ? 1 : dir;
    }

    private static double mid(OrderBookSnapshot s) {
        if (s == null || s.bids == null || s.asks == null || s.bids.isEmpty() || s.asks.isEmpty()) return 0.0;
        return (s.bids.get(0).price + s.asks.get(0).price) / 2.0;
    }

    private static double spread(OrderBookSnapshot s) {
        if (s == null || s.bids == null || s.asks == null || s.bids.isEmpty() || s.asks.isEmpty()) return 0.0;
        return Math.max(0.0, s.asks.get(0).price - s.bids.get(0).price);
    }

    private static double microPrice(OrderBookSnapshot s) {
        if (s == null || s.bids == null || s.asks == null || s.bids.isEmpty() || s.asks.isEmpty()) return 0.0;
        double bidP = s.bids.get(0).price;
        double askP = s.asks.get(0).price;
        double bidQ = Math.max(1.0, s.bids.get(0).quantity);
        double askQ = Math.max(1.0, s.asks.get(0).quantity);
        return (askP * bidQ + bidP * askQ) / (bidQ + askQ);
    }

    private static double imbalance(OrderBookSnapshot s, int levels) {
        long bid = sumQty(s.bids, levels);
        long ask = sumQty(s.asks, levels);
        return bid / (double) (bid + ask + 1L);
    }

    private static long depthProxyDelta(OrderBookSnapshot prev, OrderBookSnapshot curr, int levels, int dir) {
        long prevBid = sumQty(prev.bids, levels);
        long prevAsk = sumQty(prev.asks, levels);
        long currBid = sumQty(curr.bids, levels);
        long currAsk = sumQty(curr.asks, levels);

        long bidChg = currBid - prevBid;
        long askChg = currAsk - prevAsk;
        long raw = bidChg - askChg;

        if (dir > 0) return Math.max(0, raw);
        if (dir < 0) return Math.min(0, raw);
        return raw;
    }

    private static long topNAbsQtyChange(OrderBookSnapshot prev, OrderBookSnapshot curr, int levels) {
        long bidPrev = sumQty(prev.bids, levels);
        long askPrev = sumQty(prev.asks, levels);
        long bidCurr = sumQty(curr.bids, levels);
        long askCurr = sumQty(curr.asks, levels);
        return Math.abs(bidCurr - bidPrev) + Math.abs(askCurr - askPrev);
    }

    private static long sumQty(List<? extends Object> levelsList, int levels) {
        if (levelsList == null || levelsList.isEmpty()) return 0L;
        return levelsList.stream().limit(levels).mapToLong(x -> {
            try {
                return (long) x.getClass().getField("quantity").get(x);
            } catch (Exception e) {
                return 0L;
            }
        }).sum();
    }

    private static double safeLtp(OrderBookSnapshot s, double fallback) {
        if (s == null) return fallback;
        try {
            var f = s.getClass().getField("ltp");
            Object v = f.get(s);
            if (v instanceof Number n) return n.doubleValue();
        } catch (Exception ignored) {}
        return fallback;
    }

    private static long safeVolume(OrderBookSnapshot s) {
        if (s == null) return 0L;
        try {
            var f = s.getClass().getField("volume");
            Object v = f.get(s);
            if (v instanceof Number n) return n.longValue();
        } catch (Exception ignored) {}
        return 0L;
    }

    private static long safeOi(OrderBookSnapshot s) {
        if (s == null) return 0L;
        try {
            var f = s.getClass().getField("oi");
            Object v = f.get(s);
            if (v instanceof Number n) return n.longValue();
        } catch (Exception ignored) {}
        return 0L;
    }

    private static long safeLastQty(OrderBookSnapshot s) {
        if (s == null) return 0L;
        try {
            var f = s.getClass().getField("lastTradedQuantity");
            Object v = f.get(s);
            if (v instanceof Number n) return n.longValue();
        } catch (Exception ignored) {}
        return 0L;
    }

    private static double clamp(double z) {
        if (Double.isNaN(z) || Double.isInfinite(z)) return 0.0;
        return Math.max(0.0, Math.min(6.0, z));
    }

    private static final class State {
        long cvd;
        int lastDir;
        double lastMicro;

        final EwmaStats absDeltaStats = new EwmaStats(0.08);
        final EwmaStats dVolStats = new EwmaStats(0.10);
        final EwmaStats topNStats = new EwmaStats(0.10);
        final EwmaStats spreadStats = new EwmaStats(0.06);
        final EwmaStats imbStats = new EwmaStats(0.06);
        final EwmaStats dOiStats = new EwmaStats(0.05);
    }

    private static final class EwmaStats {
        private final double a;
        private double mean;
        private double var;

        EwmaStats(double alpha) {
            this.a = alpha;
        }

        void update(double x) {
            if (mean == 0.0 && var == 0.0) {
                mean = x;
                var = 1e-6;
                return;
            }
            double prevMean = mean;
            mean = a * x + (1.0 - a) * mean;
            double diff = x - prevMean;
            var = a * (diff * diff) + (1.0 - a) * var;
            if (var < 1e-6) var = 1e-6;
        }

        double z(double x) {
            double sd = Math.sqrt(var);
            return (x - mean) / sd;
        }
    }

    public static final class Result {
        public long token;
        public String symbol;
        public boolean usedTradeDelta;

        public long delta;
        public long cvd;

        public long dVol;
        public long dOi;

        public double spread;
        public double imbalance;

        public double micro;
        public double microShift;

        public double score;

        public double zAbsDelta;
        public double zDVol;
        public double zTopN;
        public double zSpread;
        public double zImb;
        public double zDOi;

        public boolean surge;

        static Result empty() {
            Result r = new Result();
            r.surge = false;
            return r;
        }
    }
}