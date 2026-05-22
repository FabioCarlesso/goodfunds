package com.goodfunds.exception;

import java.util.UUID;

public class DuplicateBudgetException extends RuntimeException {

    public DuplicateBudgetException(UUID categoryId, int mes, int ano) {
        super("Ja existe um orcamento para a categoria " + categoryId + " em " + mes + "/" + ano);
    }
}
