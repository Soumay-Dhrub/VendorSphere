package com.vendorsphere.common.util;

import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.exception.BusinessException;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

/**
 * Builds {@link Pageable} values for list endpoints and maps a {@link Page} onto the
 * shared {@link PageResponse} wrapper.
 *
 * <p>Requirements 31.3, 31.4 and 31.5: page defaults to 0, size defaults to 20 and is
 * clamped into {@code [1, 100]}, and a {@code sort} field outside the endpoint's
 * {@link SortWhitelist} is rejected with HTTP 400 listing the sortable fields.
 */
public final class PageSupport {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 100;
    public static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.ASC;

    private PageSupport() {
    }

    /**
     * Resolves the requested pagination parameters against an endpoint's sort whitelist.
     *
     * @param page      requested page, defaulted to 0 when absent or negative
     * @param size      requested size, defaulted to 20 when absent and clamped into {@code [1, 100]}
     * @param sort      requested sort field, defaulted to {@link SortWhitelist#defaultField()}
     *                  when absent or blank
     * @param direction requested direction, defaulted to ascending when absent or unrecognized
     * @param whitelist the sortable fields of the endpoint
     * @throws BusinessException with HTTP 400 when {@code sort} names a field outside the whitelist
     */
    public static Pageable pageable(Integer page,
                                    Integer size,
                                    String sort,
                                    String direction,
                                    SortWhitelist whitelist) {
        int resolvedPage = resolvePage(page);
        int resolvedSize = resolveSize(size);
        String resolvedSort = resolveSortField(sort, whitelist);
        Sort.Direction resolvedDirection = resolveDirection(direction);
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(resolvedDirection, resolvedSort));
    }

    /** Requirement 31.3: absent or negative page numbers become page 0. */
    public static int resolvePage(Integer page) {
        if (page == null || page < DEFAULT_PAGE) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    /** Requirements 31.3 and 31.4: absent size becomes 20, and any size is clamped into {@code [1, 100]}. */
    public static int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(MAX_SIZE, Math.max(MIN_SIZE, size));
    }

    /** Requirement 31.5: an unknown sort field is rejected with 400 listing the sortable fields. */
    public static String resolveSortField(String sort, SortWhitelist whitelist) {
        if (whitelist == null) {
            throw new IllegalArgumentException("A sort whitelist is required");
        }
        if (sort == null || sort.isBlank()) {
            return whitelist.defaultField();
        }
        String candidate = sort.trim();
        if (!whitelist.permits(candidate)) {
            throw new BusinessException(
                    "Invalid sort field: " + candidate + ". Sortable fields: " + whitelist.describe(),
                    HttpStatus.BAD_REQUEST);
        }
        return candidate;
    }

    private static Sort.Direction resolveDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return DEFAULT_DIRECTION;
        }
        return Sort.Direction.fromOptionalString(direction.trim()).orElse(DEFAULT_DIRECTION);
    }

    /** Maps a persistence page onto the shared {@link PageResponse} wrapper (Requirement 31.1). */
    public static <E, D> PageResponse<D> map(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /** Wraps a page whose content is already in its response shape. */
    public static <T> PageResponse<T> map(Page<T> page) {
        return map(page, Function.identity());
    }
}
