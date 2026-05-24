package com.goodfunds.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Resposta de {@code GET /reports/summary}: visao geral financeira do mes.
 *
 * @param ref                  mes de referencia (formato {@code yyyy-MM})
 * @param receitas             soma dos valores de transacoes do tipo RECEITA no mes
 * @param despesas             soma dos valores de transacoes do tipo DESPESA no mes
 * @param orcado               soma dos limites de orcamento cadastrados para o mes
 * @param saldo                receitas menos despesas
 * @param percentualOrcadoUsado percentual do orcamento consumido pelas despesas;
 *                              zero quando nao ha orcamento cadastrado
 */
public record SummaryResponse(
        YearMonth ref,
        BigDecimal receitas,
        BigDecimal despesas,
        BigDecimal orcado,
        BigDecimal saldo,
        BigDecimal percentualOrcadoUsado
) {
}
