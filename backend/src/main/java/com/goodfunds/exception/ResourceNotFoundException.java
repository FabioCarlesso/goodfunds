package com.goodfunds.exception;

public abstract class ResourceNotFoundException extends RuntimeException {

    private final String type;
    private final String title;

    protected ResourceNotFoundException(String message, String type, String title) {
        super(message);
        this.type = type;
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }
}
