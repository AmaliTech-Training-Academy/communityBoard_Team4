package com.amalitech.qa.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static Properties properties;
    private static final String CONFIG_FILE = "/config.properties";

    static {
        loadProperties();
    }

    /**
     * Load properties from config.properties
     */
    private static void loadProperties() {
        properties = new Properties();
        try (InputStream input = ConfigManager.class.getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("Configuration file not found: " + CONFIG_FILE);
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Get the base URL from config
     *
     * @return Base URL for API tests
     */
    public static String getBaseUrl() {
        return getProperty("base.url");
    }

    /**
     * Get admin email from config
     *
     * @return Admin email for authentication tests
     */
    public static String getAdminEmail() {
        return getProperty("admin.email");
    }

    /**
     * Get admin password from config
     *
     * @return Admin password for authentication tests
     */
    public static String getAdminPassword() {
        return getProperty("admin.password");
    }

    /**
     * Get regular user email from config
     *
     * @return User email for authentication tests
     */
    public static String getUserEmail() {
        return getProperty("user.email");
    }

    /**
     * Get regular user password from config
     *
     * @return User password for authentication tests
     */
    public static String getUserPassword() {
        return getProperty("user.password");
    }

    /**
     * Get other user email from config
     *
     * @return Other user email for multi-user test scenarios
     */
    public static String getOtherUserEmail() {
        return getProperty("other.user.email");
    }

    /**
     * Get other user password from config
     *
     * @return Other user password for multi-user test scenarios
     */
    public static String getOtherUserPassword() {
        return getProperty("other.user.password");
    }

    /**
     * Generic getter for any property.
     * Resolution order:
     *   1. JVM system property  (-Dbase.url=... on the Maven command line)
     *   2. Entry in config.properties classpath resource
     *
     * This lets CI pipelines override the default ALB URL without changing
     * the committed config.properties file.
     *
     * @param key Property key
     * @return Property value
     */
    private static String getProperty(String key) {
        // System property takes precedence (e.g. -Dbase.url=http://localhost:8080)
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Property not found in config: " + key);
        }
        return value.trim();
    }

}
