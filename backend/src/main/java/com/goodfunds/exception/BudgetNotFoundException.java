package com.goodfunds.exception;

import java.util.UUID;

public class BudgetNotFoundException extends ResourceNotFoundException {

    public BudgetNotFoundException(UUID id) {
        super("Orcamento nao encontrado: " + id, "budget-not-found", "Orcamento nao encontrado");
    }
}
