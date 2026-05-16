package br.com.cmms.cmms.exception;

/**
 * Thrown for business-level validation failures that Bean Validation cannot
 * express (cross-field checks, state transitions, weak passwords, etc.).
 * Maps to HTTP 400 Bad Request.
 *
 * <p>For DTO-level constraint violations, prefer {@code @Valid} with
 * {@code jakarta.validation} annotations — those are handled separately by
 * the global exception handler.
 */
public class ValidationException extends BusinessException {

    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
