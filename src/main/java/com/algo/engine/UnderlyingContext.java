package com.algo.engine;

import com.algo.model.OptionMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnderlyingContext {

    public final String name;        // NIFTY / BANKNIFTY
    public final long futToken;
    public final int strikeStep;

    // optionToken → OptionMeta
    public final Map<Long, OptionMeta> options = new HashMap<>();

    public UnderlyingContext(
            String name,
            long futToken,
            int strikeStep,
            List<OptionMeta> optionList) {

        this.name = name;
        this.futToken = futToken;
        this.strikeStep = strikeStep;

        optionList.forEach(o -> options.put(o.optionToken, o));
    }
}
