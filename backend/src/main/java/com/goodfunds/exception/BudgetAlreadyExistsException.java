package com.goodfunds.exception;

import java.util.UUID;

public class BudgetAlreadyExistsException extends RuntimeException {

    public BudgetAlreadyExistsException(UUID categoryId, Integer mes, Integer ano) {
        super("Ja existe um orcamento para a categoria " + categoryId
                + " no periodo " + String.format("%04d-%02d", ano, mes));
    }
}
