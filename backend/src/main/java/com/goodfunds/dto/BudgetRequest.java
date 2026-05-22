package com.goodfunds.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetRequest(
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal limite,
        @NotNull UUID categoryId,
        @NotNull @Min(1) @Max(12) Integer mes,
        @NotNull @Min(2000) @Max(2100) Integer ano
) {
}
