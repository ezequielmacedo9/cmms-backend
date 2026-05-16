package br.com.cmms.cmms.exception;

/**
 * Thrown when the caller is authenticated but lacks permission for the
 * requested operation. Maps to HTTP 403 Forbidden.
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String errorCode, String message) {
        super(errorCode, message);
    }
}
