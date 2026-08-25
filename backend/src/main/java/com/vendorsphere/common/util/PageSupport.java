package com.vendorsphere.common.util;

import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.exception.BusinessException;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

public final class PageSupport {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 100;
    public static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.ASC;

    private PageSupport() {
    }

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

    public static int resolvePage(Integer page) {
        if (page == null || page < DEFAULT_PAGE) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    public static int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(MAX_SIZE, Math.max(MIN_SIZE, size));
    }

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

    public static <T> PageResponse<T> map(Page<T> page) {
        return map(page, Function.identity());
    }
}
