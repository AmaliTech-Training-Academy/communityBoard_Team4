package com.amalitech.communityboard.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * CB-211: Application-level configuration for caching.
 * CB-212: Async task executor for fire-and-forget analytics processing.
 *
 * Uses ConcurrentMapCacheManager — an in-memory, thread-safe cache backed by
 * java.util.concurrent.ConcurrentHashMap. No external dependencies required.
 *
 * Cache regions:
 *  "posts" — paginated post lists (evicted on any create/update/delete)
 *  "post"  — single post by ID (evicted on update/delete of that specific post)
 *
 * Async executor ("analyticsExecutor"):
 *  - core pool: 2 threads always alive
 *  - max pool:  10 threads under burst load
 *  - queue:     100 tasks before new threads are spawned beyond core
 *  - thread name prefix: "async-analytics-" for easy identification in logs/thread dumps
 */
@Configuration
@EnableCaching
@EnableAsync
public class AppConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("posts", "post");
    }

    @Bean(name = "analyticsExecutor")
    public Executor analyticsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-analytics-");
        executor.initialize();
        return executor;
    }
}
