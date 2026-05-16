package com.goodfunds.dto;

import com.goodfunds.domain.FormaPagamento;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionRequest(
        @NotBlank @Size(max = 500) String descricao,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal valor,
        @NotNull LocalDate data,
        @NotNull FormaPagamento formaPagamento,
        @NotNull UUID categoryId
) {
}
