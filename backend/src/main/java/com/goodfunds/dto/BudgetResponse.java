package com.goodfunds.dto;

import com.goodfunds.domain.Budget;
import com.goodfunds.domain.TipoCategoria;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        BigDecimal limite,
        UUID categoryId,
        String categoryNome,
        TipoCategoria categoryTipo,
        Integer mes,
        Integer ano,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static BudgetResponse from(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getLimite(),
                budget.getCategory().getId(),
                budget.getCategory().getNome(),
                budget.getCategory().getTipo(),
                budget.getMes(),
                budget.getAno(),
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }
}
