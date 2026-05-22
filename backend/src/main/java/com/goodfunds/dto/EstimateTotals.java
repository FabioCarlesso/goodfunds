package com.goodfunds.dto;

import java.math.BigDecimal;

/**
 * Totais consolidados da estimativa do mes corrente, somados sobre todas as categorias.
 *
 * @param media     soma das medias dos ultimos 3 meses fechados
 * @param realizado soma do que ja foi lancado no mes corrente
 * @param projecao  soma das projecoes do mes corrente
 */
public record EstimateTotals(
        BigDecimal media,
        BigDecimal realizado,
        BigDecimal projecao
) {
}
