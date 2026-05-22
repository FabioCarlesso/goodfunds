package com.goodfunds.dto;

import com.goodfunds.domain.Budget;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        BigDecimal limite,
        CategoryResponse category,
        Integer mes,
        Integer ano,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static BudgetResponse from(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getLimite(),
                CategoryResponse.from(budget.getCategory()),
                budget.getMes(),
                budget.getAno(),
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }
}
