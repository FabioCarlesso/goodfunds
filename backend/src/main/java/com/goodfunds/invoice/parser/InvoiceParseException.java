package com.goodfunds.invoice.parser;

public class InvoiceParseException extends RuntimeException {

    public InvoiceParseException(String message) {
        super(message);
    }

    public InvoiceParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
