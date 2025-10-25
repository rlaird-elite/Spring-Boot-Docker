package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // Import AccessDeniedException
import org.springframework.validation.FieldError; // Import FieldError
import org.springframework.web.bind.MethodArgumentNotValidException; // Import MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException; // Import ResponseStatusException

import java.util.HashMap; // Import HashMap
import java.util.Map;     // Import Map

@ControllerAdvice // Makes this class apply globally to all controllers
public class GlobalExceptionHandler {

    // Handler for UserAlreadyExistsException (returns 409 Conflict)
    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<Map<String, String>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        // Return JSON: {"message": "User already exists..."}
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }

    // --- NEW: Handler for Validation Errors (returns 400 Bad Request) ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        // Return JSON: {"fieldName": "errorMessage", ...}
        return ResponseEntity.badRequest().body(errors);
    }
    // --- END NEW Handler ---


    // Handler for AccessDeniedException (returns 403 Forbidden)
    // This catches exceptions thrown by service layer tenant checks
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
    }

    // Handler for IllegalStateException (often related to auth context, returns 401 Unauthorized)
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        // Check if message indicates auth issue vs other state issues
        if (ex.getMessage() != null && ex.getMessage().contains("User must be authenticated")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Authentication required."));
        }
        // Handle other IllegalStateExceptions if necessary, or rethrow/log
        // For now, return a generic forbidden if not clearly auth related in service layer
        // Or consider a 500 if it indicates an unexpected server state
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));

    }

    // Handler for ResponseStatusException (used in controllers for specific status codes)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of("message", ex.getReason() != null ? ex.getReason() : "An error occurred"));
    }


    // Optional: Generic fallback handler for any other exceptions (returns 500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        // Log the exception for debugging
        // logger.error("An unexpected error occurred", ex); // Assuming you have a logger
        System.err.println("An unexpected error occurred: " + ex.getMessage());
        ex.printStackTrace(); // Print stack trace to console (for dev)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "An unexpected internal server error occurred."));
    }
}

