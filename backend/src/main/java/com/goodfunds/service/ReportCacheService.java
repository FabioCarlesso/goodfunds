package com.goodfunds.service;

import com.goodfunds.config.CacheConfig;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Invalidacao escopada por usuario dos caches de relatorio ({@code /reports/*}).
 *
 * <p>Os relatorios sao cacheados por chave {@code "<userId>::..."} (ver os
 * {@code @Cacheable} em {@link ReportService} e {@link EstimateService}). Como uma escrita
 * de um usuario nao deve descartar o cache dos demais, a invalidacao remove apenas as
 * entradas cujo prefixo da chave bate com o usuario, em vez de limpar o cache inteiro.</p>
 *
 * <p>Acionada pelos servicos de escrita que afetam os agregados de relatorio:
 * {@link TransactionService}, {@link BudgetService} e {@link InvoiceProcessingService}.</p>
 */
@Service
public class ReportCacheService {

    /** Separador entre o {@code userId} e o restante da chave de cache. */
    public static final String KEY_SEPARATOR = "::";

    private final CacheManager cacheManager;

    public ReportCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Remove todas as entradas de relatorio pertencentes ao usuario informado, em todos os
     * caches de {@code /reports/*}. Entradas de outros usuarios permanecem intactas.
     */
    public void evictUser(UUID userId) {
        String prefix = userId + KEY_SEPARATOR;
        for (String cacheName : CacheConfig.REPORT_CACHES) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                caffeineCache.getNativeCache().asMap().keySet()
                        .removeIf(key -> key instanceof String s && s.startsWith(prefix));
            }
        }
    }
}
