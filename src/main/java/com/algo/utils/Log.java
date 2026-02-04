package com.algo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Log {

    private static final Logger logger =
            LoggerFactory.getLogger("ALGO");

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Log() {}

    public static void info(String msg) {
        String ts = LocalDateTime.now().format(FMT);
        logger.info("[" + ts + "] " + msg);
    }

    public static void error(String msg) {
        String ts = LocalDateTime.now().format(FMT);
        logger.error("[" + ts + "] ERROR " + msg);
    }
}
