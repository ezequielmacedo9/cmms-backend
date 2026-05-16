package br.com.cmms.cmms.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Canonical paged response envelope used by every paged endpoint.
 * Hides Spring's internal {@code Page} implementation details from the API
 * and gives the frontend a stable shape.
 */
public record PagedResponseDTO<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int page,
    int size,
    boolean first,
    boolean last
) {

    public static <T> PagedResponseDTO<T> of(Page<T> page) {
        return new PagedResponseDTO<>(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize(),
            page.isFirst(),
            page.isLast()
        );
    }

    /** Converts {@code Page<S>} → {@code PagedResponseDTO<T>} using {@code mapper}. */
    public static <S, T> PagedResponseDTO<T> of(Page<S> page, Function<S, T> mapper) {
        return of(page.map(mapper));
    }
}
