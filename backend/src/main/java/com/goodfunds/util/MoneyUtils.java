package com.goodfunds.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Helpers de arredondamento monetario compartilhados pelas engines de relatorios.
 * Todos os valores sao normalizados para 2 casas decimais com {@link RoundingMode#HALF_UP}.
 */
public final class MoneyUtils {

    private MoneyUtils() {
    }

    /**
     * Normaliza o valor para 2 casas decimais (HALF_UP); {@code null} vira {@link #zero()}.
     */
    public static BigDecimal scale(BigDecimal value) {
        return value == null ? zero() : value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Zero monetario com 2 casas decimais.
     */
    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
