package com.goodfunds.exception;

/**
 * Lancada no upload quando a origem da fatura nao possui um parser registrado e,
 * portanto, nao pode ser processada. Evita salvar arquivo/registro de uma fatura
 * que fatalmente falharia no processamento.
 */
public class UnsupportedInvoiceOrigemException extends RuntimeException {

    public UnsupportedInvoiceOrigemException(String message) {
        super(message);
    }
}
