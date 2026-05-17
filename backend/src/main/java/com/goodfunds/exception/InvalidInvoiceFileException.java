package com.goodfunds.exception;

public class InvalidInvoiceFileException extends RuntimeException {

    public InvalidInvoiceFileException(String message) {
        super(message);
    }
}
