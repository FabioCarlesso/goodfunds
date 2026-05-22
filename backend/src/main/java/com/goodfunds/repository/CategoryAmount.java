package com.goodfunds.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projecao de agregacao: total de {@code valor} de transacoes somado por categoria
 * dentro de um periodo. Usado pela engine de estimativas.
 */
public record CategoryAmount(UUID categoryId, BigDecimal total) {
}
