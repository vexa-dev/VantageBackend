package com.vexa.vantage.exception;

import lombok.Getter;

@Getter
public class DependencyException extends RuntimeException {
    private final String type; // "CRITICAL_DEPENDENCY" or "REASSIGNMENT_NEEDED"

    public DependencyException(String type, String message) {
        super(message);
        this.type = type;
    }
}
