package com.papairs.auth.exception;

import com.papairs.auth.dto.response.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle authentication failures
     * @param e exception
     * @return ResponseEntity (401 Unauthorized)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<AuthResponse> handleAuthException(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.error(e.getMessage()));
    }

    /**
     * Handle attempts to register with an existing username/email
     * @param e exception
     * @return ResponseEntity 409 (Conflict)
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<AuthResponse> handleUserExists(UserAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(AuthResponse.error(e.getMessage()));
    }

    /**
     * Handle deactivated user login attempts
     * @param e exception
     * @return ResponseEntity (403 Forbidden)
     */
    @ExceptionHandler(UserDeactivatedException.class)
    public ResponseEntity<AuthResponse> handleUserDeactivated(UserDeactivatedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(AuthResponse.error(e.getMessage()));
    }

    /**
     * Handle invalid or expired tokens
     * @param e exception
     * @return ResponseEntity (401 Unauthorized)
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<AuthResponse> handleInvalidToken(InvalidTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.error(e.getMessage()));
    }

    /**
     * Handle missing or malformed Authorization header
     * @param e exception
     * @return ResponseEntity (401 Unauthorized)
     */
    @ExceptionHandler(InvalidAuthHeaderException.class)
    public ResponseEntity<AuthResponse> handleInvalidHeader(InvalidAuthHeaderException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse.error(e.getMessage()));
    }

    /**
     * Handle validation errors from @Valid annotation
     * Aggregate all field errors into a single message
     * @param e exception
     * @return ResponseEntity (400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthResponse> handleValidationException(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(AuthResponse.error(errors));
    }

    /**
     * Handle invalid JSON or missing required fields
     * @param e exception
     * @return ResponseEntity (400 Bad Request)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AuthResponse> handleInvalidJson(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(AuthResponse.error("Invalid request format"));
    }

    /**
     * Catch-all for any other exceptions
     * @param e exception
     * @return ResponseEntity (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponse> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponse.error("An unexpected error occurred"));
    }
}
