package com.goodfunds.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Entrada mensal de {@code GET /reports/evolution}: receitas e despesas totais de um mes.
 *
 * @param ref      mes de referencia (formato {@code yyyy-MM})
 * @param receitas soma dos valores de transacoes de categorias do tipo RECEITA no mes
 * @param despesas soma dos valores de transacoes de categorias do tipo DESPESA no mes
 */
public record MonthlyEntry(YearMonth ref, BigDecimal receitas, BigDecimal despesas) {
}
