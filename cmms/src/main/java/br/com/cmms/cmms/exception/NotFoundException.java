package br.com.cmms.cmms.exception;

/**
 * Thrown when a domain entity is requested by identifier and not found.
 * Maps to HTTP 404 Not Found.
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }

    /** Convenience factory for the common "resource X with id Y not found" pattern. */
    public static NotFoundException of(String resource, Object id) {
        return new NotFoundException(
            resource.toUpperCase() + "_NOT_FOUND",
            resource + " não encontrado(a) (id=" + id + ")."
        );
    }
}
