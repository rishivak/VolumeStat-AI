package com.algo.analytics;

import com.algo.enums.SignalDirection;
import com.algo.state.LastPriceCache;
import com.algo.trade.ScalpingTradeEngine;
import com.algo.utils.Log;

import java.util.HashMap;
import java.util.Map;

public class SignalCoordinator {
    private static final long CONFIRM_WINDOW_MS = 5000;
    private static final long COOLDOWN_MS = 15000;
    private static final Map<Long, PendingIgnition> pending = new HashMap<>();
    private static final Map<Long, Long> cooldowns = new HashMap<>();
    private static class PendingIgnition {
        long time;
        SignalDirection direction;
        int score;
    }
    // ==========================
    // CALLED BY IGNITION ENGINE
    // ==========================
    public static void onIgnition(long futToken, SignalDirection dir,int score) {
        long now = System.currentTimeMillis();
        // cooldown check
        Long cd = cooldowns.get(futToken);
        if (cd != null && now < cd) return;
        PendingIgnition pi = new PendingIgnition();
        pi.time = now;
        pi.direction = dir;
        pi.score = score;
        pending.put(futToken, pi);
        Log.info("🟡 IGNITION STORED | fut=" + futToken + " dir=" + dir + " score=" + score);
    }
    // ==========================
    // CALLED BY OPTION FLOW
    // ==========================
    public static void onOptionFlow(long futToken, SignalDirection flowDir,int flowScore) {
        PendingIgnition pi = pending.get(futToken);
        if (pi == null) return;
        long now = System.currentTimeMillis();
        if (now - pi.time > CONFIRM_WINDOW_MS) {
            pending.remove(futToken); // expired
            return;
        }
        if (pi.direction != flowDir) return;
        int finalScore = (pi.score + flowScore) / 2;
        // 🔥 EXACT ENTRY POINT
        ScalpingTradeEngine.tryEnter(futToken, flowDir, LastPriceCache.getFutLtp(futToken), finalScore);
        // ✅ CONFIRMED
        Log.info("✅ CONFIRMED SIGNAL | fut=" + futToken + " dir=" + flowDir + " confidence=" + finalScore);
        pending.remove(futToken);
        cooldowns.put(futToken, now + COOLDOWN_MS);
    }
}
