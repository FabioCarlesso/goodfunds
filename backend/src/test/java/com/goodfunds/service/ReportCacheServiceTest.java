package com.goodfunds.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.goodfunds.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportCacheServiceTest {

    private CaffeineCacheManager cacheManager;
    private ReportCacheService service;

    @BeforeEach
    void setUp() {
        cacheManager = new CaffeineCacheManager(CacheConfig.REPORT_CACHES.toArray(String[]::new));
        cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(100));
        service = new ReportCacheService(cacheManager);
    }

    @Test
    void evictUser_removesOnlyEntriesOfTheUser_whenNoTransactionActive() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        for (String cacheName : CacheConfig.REPORT_CACHES) {
            Cache cache = cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            cache.put(keyFor(userA, "k1"), "valueA1");
            cache.put(keyFor(userA, "k2"), "valueA2");
            cache.put(keyFor(userB, "k1"), "valueB1");
        }

        service.evictUser(userA);

        for (String cacheName : CacheConfig.REPORT_CACHES) {
            CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            assertThat(cache.getNativeCache().asMap()).containsOnlyKeys(keyFor(userB, "k1"));
        }
    }

    @Test
    void evictUser_isIdempotent_whenCacheHasNoMatchingEntries() {
        UUID userId = UUID.randomUUID();

        service.evictUser(userId);

        for (String cacheName : CacheConfig.REPORT_CACHES) {
            CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            assertThat(cache.getNativeCache().asMap()).isEmpty();
        }
    }

    @Test
    void evictUser_defersEvictionUntilAfterCommit_whenSynchronizationActive() {
        UUID userId = UUID.randomUUID();
        for (String cacheName : CacheConfig.REPORT_CACHES) {
            Cache cache = cacheManager.getCache(cacheName);
            assertThat(cache).isNotNull();
            cache.put(keyFor(userId, "k1"), "value");
        }

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.evictUser(userId);

            for (String cacheName : CacheConfig.REPORT_CACHES) {
                CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheName);
                assertThat(cache).isNotNull();
                assertThat(cache.getNativeCache().asMap())
                        .as("entries should still be present before commit")
                        .containsKey(keyFor(userId, "k1"));
            }

            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }

            for (String cacheName : CacheConfig.REPORT_CACHES) {
                CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheName);
                assertThat(cache).isNotNull();
                assertThat(cache.getNativeCache().asMap()).isEmpty();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void evictUser_ignoresMissingOrNonCaffeineCaches() {
        CaffeineCacheManager emptyManager = new CaffeineCacheManager();
        emptyManager.setCacheNames(java.util.List.of());
        ReportCacheService isolated = new ReportCacheService(emptyManager);

        isolated.evictUser(UUID.randomUUID());
    }

    private static String keyFor(UUID userId, String suffix) {
        return userId + ReportCacheService.KEY_SEPARATOR + suffix;
    }
}
