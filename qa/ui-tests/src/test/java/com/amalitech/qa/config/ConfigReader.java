package com.amalitech.qa.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads test configuration from {@code src/test/resources/config.properties}.
 * All keys are accessible via {@link #get(String)}, with typed helpers for
 * the most commonly used values.
 */
public final class ConfigReader {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = ConfigReader.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new RuntimeException(
                        "config.properties not found on the test classpath");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    private ConfigReader() {}

    /** Returns the raw string value for {@code key}, or {@code null} if absent. */
    public static String get(String key) {
        return PROPS.getProperty(key);
    }

    /** Returns the raw string value for {@code key}, falling back to {@code defaultValue}. */
    public static String get(String key, String defaultValue) {
        return PROPS.getProperty(key, defaultValue);
    }

    public static String getBaseUrl() {
        return get("base.url",
                "http://community-board-alb-1961944079.eu-west-1.elb.amazonaws.com");
    }

    public static String getValidEmail() {
        return get("valid.email");
    }

    public static String getValidPassword() {
        return get("valid.password");
    }

    public static boolean isHeadless() {
        String sysProp = System.getProperty("headless");
        if (sysProp != null) {
            return Boolean.parseBoolean(sysProp);
        }
        return Boolean.parseBoolean(get("headless", "true"));
    }

    public static int getImplicitWait() {
        return Integer.parseInt(get("implicit.wait", "10"));
    }

    public static int getExplicitWait() {
        return Integer.parseInt(get("explicit.wait", "15"));
    }
}
