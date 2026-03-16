package io.github.bananachocohaim.pointassignment2603.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String POLICY_CACHE = "policies";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(POLICY_CACHE);
    }
}
