package br.com.cmms.cmms.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GlobalExceptionHandler}. Asserts the
 * exception → HTTP status mapping and the envelope shape so the contract
 * with the frontend (ApiError) cannot regress silently.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        MockHttpServletRequest mock = new MockHttpServletRequest("GET", "/api/test");
        request = mock;
    }

    @Test
    @DisplayName("NotFoundException → 404 + code domínio")
    void handlesNotFound() {
        ResponseEntity<ApiError> r = handler.handleNotFound(
            NotFoundException.of("Máquina", 42L), request);
        assertEnvelope(r, HttpStatus.NOT_FOUND, "MÁQUINA_NOT_FOUND");
        assertThat(r.getBody().message()).contains("42");
    }

    @Test
    @DisplayName("ConflictException → 409")
    void handlesConflict() {
        ResponseEntity<ApiError> r = handler.handleConflict(
            new ConflictException("DUPLICATE_X", "Já existe."), request);
        assertEnvelope(r, HttpStatus.CONFLICT, "DUPLICATE_X");
    }

    @Test
    @DisplayName("UnauthorizedException → 401")
    void handlesUnauthorized() {
        ResponseEntity<ApiError> r = handler.handleUnauthorized(
            new UnauthorizedException("BAD_CREDENTIALS", "Email ou senha incorretos."), request);
        assertEnvelope(r, HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS");
    }

    @Test
    @DisplayName("ForbiddenException → 403")
    void handlesForbidden() {
        ResponseEntity<ApiError> r = handler.handleForbidden(
            new ForbiddenException("ACCOUNT_LOCKED", "Conta bloqueada."), request);
        assertEnvelope(r, HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED");
    }

    @Test
    @DisplayName("ValidationException → 400 com code de domínio")
    void handlesBusinessValidation() {
        ResponseEntity<ApiError> r = handler.handleBusinessValidation(
            new ValidationException("INVALID_ROLE", "Role inválida."), request);
        assertEnvelope(r, HttpStatus.BAD_REQUEST, "INVALID_ROLE");
    }

    @Test
    @DisplayName("BadCredentialsException do Spring → 401 BAD_CREDENTIALS")
    void handlesSpringBadCredentials() {
        ResponseEntity<ApiError> r = handler.handleBadCredentials(
            new BadCredentialsException("..."), request);
        assertEnvelope(r, HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS");
    }

    @Test
    @DisplayName("AccessDeniedException → 403 ACCESS_DENIED")
    void handlesAccessDenied() {
        ResponseEntity<ApiError> r = handler.handleAccessDenied(
            new AccessDeniedException("nope"), request);
        assertEnvelope(r, HttpStatus.FORBIDDEN, "ACCESS_DENIED");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException → 400 MALFORMED_JSON")
    void handlesUnreadable() {
        ResponseEntity<ApiError> r = handler.handleUnreadable(
            new HttpMessageNotReadableException("bad json", (org.springframework.http.HttpInputMessage) null), request);
        assertEnvelope(r, HttpStatus.BAD_REQUEST, "MALFORMED_JSON");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException → 400 MISSING_PARAMETER")
    void handlesMissingParam() {
        ResponseEntity<ApiError> r = handler.handleMissingParam(
            new MissingServletRequestParameterException("page", "int"), request);
        assertEnvelope(r, HttpStatus.BAD_REQUEST, "MISSING_PARAMETER");
        assertThat(r.getBody().message()).contains("page");
    }

    @Test
    @DisplayName("ConstraintViolationException → 400 VALIDATION_FAILED")
    void handlesConstraintViolation() {
        // Empty set is enough to validate the mapping; field extraction is
        // exercised by Bean Validation paths.
        ResponseEntity<ApiError> r = handler.handleConstraintViolation(
            new ConstraintViolationException("validation failed", Set.of()), request);
        assertEnvelope(r, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
    }

    @Test
    @DisplayName("DataIntegrityViolationException → 409 DATA_INTEGRITY_VIOLATION")
    void handlesDataIntegrity() {
        ResponseEntity<ApiError> r = handler.handleDataIntegrity(
            new DataIntegrityViolationException("dup"), request);
        assertEnvelope(r, HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION");
    }

    @Test
    @DisplayName("ResponseStatusException preserva status original")
    void handlesResponseStatus() {
        ResponseEntity<ApiError> r = handler.handleResponseStatus(
            new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "billing"), request);
        assertThat(r.getStatusCode().value()).isEqualTo(402);
        assertThat(r.getBody().code()).isEqualTo("HTTP_402");
    }

    @Test
    @DisplayName("NoSuchElementException legado → 404 RESOURCE_NOT_FOUND")
    void handlesNoSuchElement() {
        ResponseEntity<ApiError> r = handler.handleNoSuchElement(
            new NoSuchElementException("gone"), request);
        assertEnvelope(r, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("Exception genérica → 500 INTERNAL_ERROR sem vazar detalhes")
    void handlesUnexpected() {
        ResponseEntity<ApiError> r = handler.handleUnexpected(
            new RuntimeException("npe deep in the stack"), request);
        assertEnvelope(r, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
        assertThat(r.getBody().message()).doesNotContain("npe");
    }

    @Test
    @DisplayName("Envelope: traceId nunca é nulo e path é a URI da request")
    void envelopeAlwaysHasTraceIdAndPath() {
        ResponseEntity<ApiError> r = handler.handleNotFound(
            NotFoundException.of("X", 1L), request);
        assertThat(r.getBody().traceId()).isNotBlank();
        assertThat(r.getBody().path()).isEqualTo("/api/test");
        assertThat(r.getBody().timestamp()).isNotNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static void assertEnvelope(ResponseEntity<ApiError> response, HttpStatus expected, String expectedCode) {
        assertThat(response.getStatusCode()).isEqualTo(expected);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(expected.value());
        assertThat(body.code()).isEqualTo(expectedCode);
        assertThat(body.error()).isEqualTo(expected.getReasonPhrase());
        assertThat(body.message()).isNotBlank();
        assertThat(body.path()).isEqualTo("/api/test");
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.traceId()).isNotBlank();
    }
}
