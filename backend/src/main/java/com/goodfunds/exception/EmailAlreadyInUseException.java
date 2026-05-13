package com.goodfunds.exception;

public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException(String email) {
        super("E-mail ja cadastrado: " + email);
    }
}
