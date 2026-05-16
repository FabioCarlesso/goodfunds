package com.goodfunds.exception;

import java.util.UUID;

public class TransactionNotFoundException extends ResourceNotFoundException {

    public TransactionNotFoundException(UUID id) {
        super("Transacao nao encontrada: " + id, "transaction-not-found", "Transacao nao encontrada");
    }
}
