package br.com.cmms.cmms.exception;

/**
 * Root of the application's business exception hierarchy.
 *
 * <p>Every business exception carries:
 * <ul>
 *   <li>An {@code errorCode} — stable, machine-readable identifier consumed
 *       by the frontend to drive UI behaviour (translations, retries,
 *       redirects). Format: SCREAMING_SNAKE_CASE, scoped by domain
 *       (e.g. {@code MAQUINA_NOT_FOUND}, {@code PASSWORD_TOO_WEAK}).</li>
 *   <li>A {@code message} — user-facing description in Portuguese (pt-BR).
 *       Safe to display in toasts and dialogs. Must NEVER contain internal
 *       implementation details or stack traces.</li>
 * </ul>
 *
 * <p>The HTTP status is decided by the global exception handler based on the
 * concrete subclass, not by the throwing site. This keeps domain code free
 * from HTTP concerns.
 */
public abstract class BusinessException extends RuntimeException {

    private final String errorCode;

    protected BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
