package com.goodfunds.repository.projection;

import java.math.BigDecimal;

/**
 * Projecao de agregacao: total de {@code valor} de transacoes somado por mes e tipo de categoria.
 * Usado pela engine de evolucao mensal.
 */
public record MonthAmount(Integer year, Integer month, BigDecimal total) {
}
