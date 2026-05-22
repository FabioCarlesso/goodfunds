package com.goodfunds.dto;

import java.time.YearMonth;
import java.util.List;

/**
 * Resposta de {@code GET /reports/estimate}: projecao do mes corrente baseada no historico.
 *
 * @param ref            mes corrente de referencia (formato {@code yyyy-MM})
 * @param diasNoMes      total de dias do mes corrente
 * @param diasDecorridos dias decorridos do mes corrente ate a data de referencia
 * @param consolidado    totais consolidados sobre todas as categorias
 * @param categorias     estimativa por categoria, ordenada por nome
 */
public record EstimateResponse(
        YearMonth ref,
        int diasNoMes,
        int diasDecorridos,
        EstimateTotals consolidado,
        List<CategoryEstimate> categorias
) {
}
