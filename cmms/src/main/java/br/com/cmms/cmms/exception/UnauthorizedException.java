package br.com.cmms.cmms.exception;

/**
 * Thrown when authentication is missing or invalid (bad credentials, expired
 * token, missing token). Maps to HTTP 401 Unauthorized.
 *
 * <p>Distinct from {@link ForbiddenException} (caller is authenticated but
 * lacks permission).
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, message);
    }
}
