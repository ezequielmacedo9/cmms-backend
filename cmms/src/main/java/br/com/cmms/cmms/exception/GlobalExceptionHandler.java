package br.com.cmms.cmms.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Centralised HTTP error translation.
 *
 * <p>Every exception that escapes a controller lands here. The handler:
 * <ol>
 *   <li>Maps the exception to an HTTP status using the type system, not
 *       string matching on messages.</li>
 *   <li>Builds a stable {@link ApiError} envelope. The {@code code} field is
 *       what clients should react to; {@code message} is for humans.</li>
 *   <li>Logs server errors with full stack trace but never leaks them to
 *       the client. Client errors are logged at INFO without trace.</li>
 *   <li>Propagates the request {@code traceId} (from MDC if present, else
 *       generates a fresh one) so frontend and backend logs can be
 *       correlated by a single id.</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MDC_TRACE_KEY = "traceId";

    // ── Domain hierarchy ─────────────────────────────────────────────────

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage(), req, null, ex, false);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest req) {
        return respond(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage(), req, null, ex, false);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, ex.getErrorCode(), ex.getMessage(), req, null, ex, false);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        return respond(HttpStatus.FORBIDDEN, ex.getErrorCode(), ex.getMessage(), req, null, ex, false);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(ValidationException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), req, null, ex, false);
    }

    @ExceptionHandler(BusinessException.class) // fallback for subclasses not covered above
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), req, null, ex, false);
    }

    // ── Spring framework exceptions ──────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException ex,
                                                         HttpServletRequest req) {
        List<ApiError.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
            "Dados inválidos. Verifique os campos destacados.", req, fields, ex, false);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest req) {
        List<ApiError.FieldError> fields = ex.getConstraintViolations().stream()
            .map(this::toFieldError)
            .toList();
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
            "Dados inválidos. Verifique os campos destacados.", req, fields, ex, false);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex,
                                                     HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "MALFORMED_JSON",
            "Corpo da requisição inválido ou malformado.", req, null, ex, false);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest req) {
        String required = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valor válido";
        String message = "Parâmetro '" + ex.getName() + "' deve ser um " + required + ".";
        return respond(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", message, req, null, ex, false);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex,
                                                       HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
            "Parâmetro obrigatório ausente: '" + ex.getParameterName() + "'.", req, null, ex, false);
    }

    // ── Security ─────────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS",
            "Email ou senha inválidos.", req, null, ex, false);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthFailure(AuthenticationException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
            "Não autenticado.", req, null, ex, false);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return respond(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
            "Você não tem permissão para executar esta operação.", req, null, ex, false);
    }

    // ── Persistence ──────────────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest req) {
        // Most common cause: unique constraint violation. Surface a generic
        // conflict — the specific service should throw ConflictException
        // beforehand whenever it can detect the duplicate explicitly.
        return respond(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
            "Operação viola uma restrição de integridade dos dados.", req, null, ex, true);
    }

    // ── Legacy / generic fallbacks ───────────────────────────────────────

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return respond(status, "HTTP_" + status.value(), message, req, null, ex, status.is5xxServerError());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNoSuchElement(NoSuchElementException ex, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
            ex.getMessage() != null ? ex.getMessage() : "Recurso não encontrado.", req, null, ex, false);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "ILLEGAL_ARGUMENT",
            ex.getMessage() != null ? ex.getMessage() : "Argumento inválido.", req, null, ex, false);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        // Treated as 409 — caller asked for an operation incompatible with current state.
        return respond(HttpStatus.CONFLICT, "ILLEGAL_STATE",
            ex.getMessage() != null ? ex.getMessage() : "Operação incompatível com o estado atual.",
            req, null, ex, false);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        // Never leak implementation details to the client on a server-side fault.
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
            "Erro interno do servidor.", req, null, ex, true);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ResponseEntity<ApiError> respond(HttpStatus status,
                                             String code,
                                             String message,
                                             HttpServletRequest req,
                                             List<ApiError.FieldError> fieldErrors,
                                             Throwable ex,
                                             boolean serverFault) {
        String traceId = currentTraceId();
        if (serverFault) {
            log.error("[{}] {} {} -> {} {}", traceId, req.getMethod(), req.getRequestURI(),
                status.value(), code, ex);
        } else {
            log.info("[{}] {} {} -> {} {} ({})", traceId, req.getMethod(), req.getRequestURI(),
                status.value(), code, message);
        }
        ApiError body = new ApiError(
            OffsetDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            code,
            message,
            req.getRequestURI(),
            traceId,
            fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }

    private static String currentTraceId() {
        String fromMdc = MDC.get(MDC_TRACE_KEY);
        return fromMdc != null ? fromMdc : UUID.randomUUID().toString();
    }

    private ApiError.FieldError toFieldError(ConstraintViolation<?> v) {
        String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "";
        // Keep just the leaf segment (e.g. "method.arg.field" -> "field")
        int lastDot = path.lastIndexOf('.');
        String field = lastDot >= 0 ? path.substring(lastDot + 1) : path;
        return new ApiError.FieldError(field, v.getMessage());
    }
}
