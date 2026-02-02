package com.algo.engine;

import com.algo.enums.SignalDirection;

public class SignalScore {

    public final SignalDirection direction;
    public final int score; // 0 - 100

    public SignalScore(SignalDirection direction, int score) {
        this.direction = direction;
        this.score = score;
    }
}
