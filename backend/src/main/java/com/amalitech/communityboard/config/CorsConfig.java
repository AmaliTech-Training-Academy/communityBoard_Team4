package com.amalitech.communityboard.config;

/**
 * CORS is now configured centrally in SecurityConfig#corsConfigurationSource().
 * This class is intentionally empty to avoid duplicate CORS configuration.
 *
 * The previous WebMvcConfigurer implementation conflicted with the SecurityConfig CORS bean
 * (both were defining allowed origins independently). Consolidating into SecurityConfig
 * ensures a single, consistent CORS policy across all routes including secured endpoints.
 */
public class CorsConfig {
    // CORS configuration lives in SecurityConfig.corsConfigurationSource()
}
