package com.goodfunds.exception;

import java.util.UUID;

public class InvoiceNotFoundException extends ResourceNotFoundException {

    public InvoiceNotFoundException(UUID id) {
        super("Fatura nao encontrada: " + id, "invoice-not-found", "Fatura nao encontrada");
    }
}
