package com.company.mdm.controller;

import com.company.mdm.dto.RegistrationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * 
 * Handles exceptions across all controllers
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RegistrationResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation error: {}", errors);

        RegistrationResponse response = new RegistrationResponse(
                false,
                "Validation error: " + errors.toString(),
                null,
                null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle IllegalStateException (invalid status transitions)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<RegistrationResponse> handleIllegalStateException(
            IllegalStateException ex) {
        log.error("Illegal state: {}", ex.getMessage());

        RegistrationResponse response = new RegistrationResponse(
                false,
                ex.getMessage(),
                null,
                null);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RegistrationResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        RegistrationResponse response = new RegistrationResponse(
                false,
                "Internal server error",
                null,
                null);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
