package com.goodfunds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origens autorizadas a consumir a API via browser (CORS).
 *
 * Configuravel por {@code app.cors.allowed-origins} (lista separada por virgula via
 * {@code APP_CORS_ALLOWED_ORIGINS}). O default cobre o dev server do Vite; em producao
 * deve ser definido com a origem real do frontend.
 */
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of("http://localhost:5173");

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
