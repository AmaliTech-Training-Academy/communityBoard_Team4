package com.amalitech.qa.testdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * TestDataLoader loads test fixtures from JSON files in the test resources
 * Caches loaded data to avoid repeated file I/O
 */
public class TestDataLoader {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Map<String, Object> cache = new HashMap<>();

    /**
     * Load JSON test data from classpath and deserialize to specified class
     *
     * @param resourcePath Path to JSON file relative to testdata folder (e.g., "auth/valid-register.json")
     * @param targetClass Class to deserialize JSON into
     * @param <T> Generic type parameter
     * @return Deserialized object of type T
     */
    public static <T> T loadTestData(String resourcePath, Class<T> targetClass) {
        String cacheKey = resourcePath + ":" + targetClass.getSimpleName();

        // Check cache first
        if (cache.containsKey(cacheKey)) {
            return (T) cache.get(cacheKey);
        }

        try {
            String fullPath = "/testdata/" + resourcePath;
            InputStream inputStream = TestDataLoader.class.getResourceAsStream(fullPath);

            if (inputStream == null) {
                throw new RuntimeException("Test data file not found: " + fullPath);
            }

            T data = mapper.readValue(inputStream, targetClass);
            cache.put(cacheKey, data);
            return data;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test data from: " + resourcePath, e);
        }
    }

    /**
     * Clear the cache (useful for test isolation if needed)
     */
    public static void clearCache() {
        cache.clear();
    }
}
