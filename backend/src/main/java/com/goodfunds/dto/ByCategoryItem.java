package com.goodfunds.dto;

import com.goodfunds.domain.TipoCategoria;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item de {@code GET /reports/by-category}: total de transacoes por categoria no mes de referencia.
 *
 * @param categoryId identificador da categoria
 * @param nome       nome da categoria
 * @param tipo       tipo da categoria ({@code RECEITA} ou {@code DESPESA}), para o consumidor
 *                   distinguir entradas de saidas
 * @param total      soma dos valores das transacoes no mes
 */
public record ByCategoryItem(UUID categoryId, String nome, TipoCategoria tipo, BigDecimal total) {
}
