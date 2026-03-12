package com.amalitech.communityboard.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CB-211: Application-level configuration for caching.
 *
 * Uses ConcurrentMapCacheManager — an in-memory, thread-safe cache backed by
 * java.util.concurrent.ConcurrentHashMap. No external dependencies required.
 *
 * Cache regions:
 *  "posts" — paginated post lists (evicted on any create/update/delete)
 *  "post"  — single post by ID (evicted on update/delete of that specific post)
 */
@Configuration
@EnableCaching
public class AppConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "posts", "post",
                "analyticsSummary",
                "analyticsPostsByCategory",
                "analyticsPostsByDay",
                "analyticsTopContributors"
        );
    }
}
