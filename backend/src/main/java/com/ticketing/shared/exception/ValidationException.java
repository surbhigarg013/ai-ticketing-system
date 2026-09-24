package com.ticketing.shared.exception;

import java.util.List;

public class ValidationException extends RuntimeException {

    private final List<FieldViolation> violations;

    public ValidationException(String message) {
        super(message);
        this.violations = List.of();
    }

    public ValidationException(String message, List<FieldViolation> violations) {
        super(message);
        this.violations = violations;
    }

    public List<FieldViolation> getViolations() {
        return violations;
    }

    public record FieldViolation(String field, String message) {}
}
