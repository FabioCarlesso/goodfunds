package com.goodfunds.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransactionCategoryRequest(
        @NotNull UUID categoryId
) {
}
