package br.com.cmms.cmms.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Standard error envelope returned by the API for every non-2xx response.
 *
 * <p>Schema (stable contract — do not break without coordinated rollout):
 * <pre>
 * {
 *   "timestamp": "2026-05-16T18:00:00Z",
 *   "status":    404,
 *   "error":     "Not Found",
 *   "code":      "MAQUINA_NOT_FOUND",
 *   "message":   "Máquina não encontrada (id=42).",
 *   "path":      "/api/maquinas/42",
 *   "traceId":   "00000000-...",
 *   "fieldErrors": [ { "field": "email", "message": "..." } ]
 * }
 * </pre>
 *
 * <p>{@code fieldErrors} is only populated for validation failures and is
 * omitted from the JSON when empty.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
    OffsetDateTime timestamp,
    int status,
    String error,
    String code,
    String message,
    String path,
    String traceId,
    List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {}
}
