package com.algo.config;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream is =
                     AppConfig.class
                             .getClassLoader()
                             .getResourceAsStream("application.properties")) {

            if (is == null) {
                throw new RuntimeException("application.properties not found");
            }
            props.load(is);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static long getLong(String key) {
        return Long.parseLong(get(key));
    }

    public static double getDouble(String key) {
        return Double.parseDouble(get(key));
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }
    public static boolean getBoolean(String key) {return Boolean.parseBoolean(get(key)); }
    public static int getInt(String key, int def) {
        String v = get(key);
        return v == null ? def : Integer.parseInt(v);
    }
    public static double getDouble(String key, double def) {
        String v = get(key);
        return v == null ? def : Double.parseDouble(v);
    }
}
