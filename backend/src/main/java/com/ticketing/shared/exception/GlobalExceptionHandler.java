package com.ticketing.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI VALIDATION_TYPE = URI.create("https://api.example.com/problems/validation-error");
    private static final URI NOT_FOUND_TYPE = URI.create("https://api.example.com/problems/not-found");
    private static final URI STATE_CONFLICT_TYPE =
            URI.create("https://api.example.com/problems/invalid-state-transition");
    private static final URI RESOLUTION_REQUIRED_TYPE =
            URI.create("https://api.example.com/problems/resolution-required");
    private static final URI INTERNAL_TYPE = URI.create("https://api.example.com/problems/internal-error");
    private static final URI AI_UNAVAILABLE_TYPE =
            URI.create("https://api.example.com/problems/ai-service-unavailable");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBeanValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "One or more fields failed validation");
        problem.setType(VALIDATION_TYPE);
        problem.setTitle("Validation Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errors", toFieldErrors(ex));
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(ValidationException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("resolution")) {
            problem.setType(RESOLUTION_REQUIRED_TYPE);
            problem.setTitle("Resolution Required");
        } else {
            problem.setType(VALIDATION_TYPE);
            problem.setTitle("Validation Error");
        }
        problem.setInstance(URI.create(request.getRequestURI()));
        if (!ex.getViolations().isEmpty()) {
            problem.setProperty(
                    "errors",
                    ex.getViolations().stream()
                            .map(v -> Map.of("field", v.field(), "message", v.message()))
                            .toList());
        }
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(NOT_FOUND_TYPE);
        problem.setTitle("Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ProblemDetail> handleStateConflict(
            InvalidStateTransitionException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(STATE_CONFLICT_TYPE);
        problem.setTitle("Invalid State Transition");
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleAiUnavailable(
            AiServiceUnavailableException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setType(AI_UNAVAILABLE_TYPE);
        problem.setTitle("AI Service Unavailable");
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(INTERNAL_TYPE);
        problem.setTitle("Internal Server Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private static List<Map<String, String>> toFieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .collect(Collectors.toList());
    }

    private static Map<String, String> toFieldError(FieldError error) {
        return Map.of("field", error.getField(), "message", error.getDefaultMessage() != null
                ? error.getDefaultMessage()
                : "Invalid value");
    }
}
