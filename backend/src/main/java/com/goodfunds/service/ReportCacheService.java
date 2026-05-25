package com.goodfunds.service;

import com.goodfunds.config.CacheConfig;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
 * {@link TransactionService}, {@link BudgetService}, {@link CategoryService} e
 * {@link InvoiceProcessingService}.</p>
 *
 * <p>Quando ha uma transacao ativa, a invalidacao e adiada para {@code afterCommit}: evitar
 * remover antes do commit fecha a janela em que uma leitura concorrente do mesmo usuario
 * leria as linhas antigas (ainda commitadas) e repovoaria o cache com dados obsoletos. Sem
 * transacao ativa (ex.: chamada direta em testes), a invalidacao e imediata.</p>
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
     *
     * <p>Se houver transacao ativa, a remocao ocorre apos o commit; caso contrario, imediatamente.</p>
     */
    public void evictUser(UUID userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictNow(userId);
                }
            });
        } else {
            evictNow(userId);
        }
    }

    private void evictNow(UUID userId) {
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
