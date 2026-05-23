package com.goodfunds.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Item de {@code GET /reports/by-category}: total de transacoes por categoria no mes de referencia.
 *
 * @param categoryId identificador da categoria
 * @param nome       nome da categoria
 * @param total      soma dos valores das transacoes no mes
 */
public record ByCategoryItem(UUID categoryId, String nome, BigDecimal total) {
}
