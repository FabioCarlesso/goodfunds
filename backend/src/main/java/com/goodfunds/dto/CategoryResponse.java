package com.goodfunds.dto;

import com.goodfunds.domain.Category;
import com.goodfunds.domain.TipoCategoria;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String nome,
        TipoCategoria tipo
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getNome(), category.getTipo());
    }
}
