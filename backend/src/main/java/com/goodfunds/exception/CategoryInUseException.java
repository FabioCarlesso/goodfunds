package com.goodfunds.exception;

import java.util.UUID;

public class CategoryInUseException extends RuntimeException {

    private final UUID categoryId;

    public CategoryInUseException(UUID categoryId) {
        super("Categoria em uso por transacoes ou orcamentos: " + categoryId);
        this.categoryId = categoryId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }
}
