package com.goodfunds.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Configuracao do cache Caffeine aplicado aos relatorios ({@code /reports/*}).
 *
 * <p>Relatorios sao leitura intensiva e recalculam agregacoes sobre transacoes e
 * orcamentos a cada chamada. O cache evita reexecutar essas consultas enquanto os
 * dados do usuario nao mudam. A invalidacao por usuario fica a cargo de
 * {@link com.goodfunds.service.ReportCacheService}, acionada quando uma
 * {@code Transaction} ou {@code Budget} e criada, editada ou removida.</p>
 *
 * <p>O {@code expireAfterWrite} funciona como rede de seguranca: garante que
 * entradas dependentes do "mes corrente" (ex.: {@code summary} sem {@code ref} e
 * {@code estimate}) sejam recalculadas apos a virada do mes, mesmo sem escrita.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Cache de {@code GET /reports/summary}. */
    public static final String REPORTS_SUMMARY = "reportsSummary";
    /** Cache de {@code GET /reports/by-category}. */
    public static final String REPORTS_BY_CATEGORY = "reportsByCategory";
    /** Cache de {@code GET /reports/evolution}. */
    public static final String REPORTS_EVOLUTION = "reportsEvolution";
    /** Cache de {@code GET /reports/estimate}. */
    public static final String REPORTS_ESTIMATE = "reportsEstimate";

    /** Caches de relatorio sujeitos a invalidacao por usuario. */
    public static final List<String> REPORT_CACHES = List.of(
            REPORTS_SUMMARY, REPORTS_BY_CATEGORY, REPORTS_EVOLUTION, REPORTS_ESTIMATE);

    /** Tempo de vida de cada entrada apos a escrita (rede de seguranca para virada de mes). */
    private static final Duration TTL = Duration.ofMinutes(10);

    /** Teto de entradas por cache; evita crescimento ilimitado com muitos usuarios/periodos. */
    private static final long MAX_SIZE = 1_000;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(REPORT_CACHES.toArray(String[]::new));
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(TTL)
                .maximumSize(MAX_SIZE));
        return manager;
    }
}
