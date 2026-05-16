package br.com.cmms.cmms.exception;

/**
 * Thrown when an operation would violate a business invariant or unique
 * constraint (duplicate email, code, etc.). Maps to HTTP 409 Conflict.
 */
public class ConflictException extends BusinessException {

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
