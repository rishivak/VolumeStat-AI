package com.algo.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Log {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Log() {}

    public static void info(String msg) {
        String ts = LocalDateTime.now().format(FMT);
        System.out.println("[" + ts + "] " + msg);
    }

    public static void error(String msg) {
        String ts = LocalDateTime.now().format(FMT);
        System.err.println("[" + ts + "] ERROR " + msg);
    }
}
