package com.goodfunds.exception;

import java.util.UUID;

public class CategoryNotFoundException extends ResourceNotFoundException {

    public CategoryNotFoundException(UUID id) {
        super("Categoria nao encontrada: " + id, "category-not-found", "Categoria nao encontrada");
    }
}
