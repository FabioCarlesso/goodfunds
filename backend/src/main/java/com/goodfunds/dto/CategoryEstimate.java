package com.goodfunds.dto;

import com.goodfunds.domain.TipoCategoria;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Estimativa do mes corrente para uma categoria.
 *
 * @param categoryId   identificador da categoria
 * @param categoryNome nome da categoria
 * @param categoryTipo tipo da categoria (RECEITA ou DESPESA)
 * @param media        media dos ultimos 3 meses fechados (soma do periodo dividida por 3)
 * @param realizado    valor ja lancado no mes corrente ate a data de referencia
 * @param projecao     projecao do total do mes corrente extrapolando o realizado parcial
 */
public record CategoryEstimate(
        UUID categoryId,
        String categoryNome,
        TipoCategoria categoryTipo,
        BigDecimal media,
        BigDecimal realizado,
        BigDecimal projecao
) {
}
