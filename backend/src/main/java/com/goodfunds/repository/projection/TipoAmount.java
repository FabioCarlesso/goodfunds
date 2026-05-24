package com.goodfunds.repository.projection;

import com.goodfunds.domain.TipoCategoria;

import java.math.BigDecimal;

/**
 * Projecao de agregacao: total de {@code valor} de transacoes somado por tipo de categoria
 * dentro de um periodo. Usado pelo resumo mensal.
 */
public record TipoAmount(TipoCategoria tipo, BigDecimal total) {
}
