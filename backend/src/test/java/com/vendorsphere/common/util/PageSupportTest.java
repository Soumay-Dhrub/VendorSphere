package com.vendorsphere.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vendorsphere.common.dto.PageResponse;
import com.vendorsphere.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

class PageSupportTest {

    private static final SortWhitelist WHITELIST =
            SortWhitelist.of("companyName", "registeredAt", "rating", "status");

    @Test
    void appliesPageAndSizeDefaultsWhenParametersAreAbsent() {
        Pageable pageable = PageSupport.pageable(null, null, null, null, WHITELIST);

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "companyName"));
    }

    @Test
    void treatsNegativePageAsZero() {
        assertThat(PageSupport.pageable(-3, 10, null, null, WHITELIST).getPageNumber()).isZero();
    }

    @Test
    void clampsSizeIntoOneToOneHundred() {
        assertThat(PageSupport.pageable(0, 250, null, null, WHITELIST).getPageSize()).isEqualTo(100);
        assertThat(PageSupport.pageable(0, 0, null, null, WHITELIST).getPageSize()).isEqualTo(1);
        assertThat(PageSupport.pageable(0, -5, null, null, WHITELIST).getPageSize()).isEqualTo(1);
        assertThat(PageSupport.pageable(0, 100, null, null, WHITELIST).getPageSize()).isEqualTo(100);
    }

    @Test
    void honoursWhitelistedSortAndDirection() {
        Pageable pageable = PageSupport.pageable(2, 50, "rating", "desc", WHITELIST);

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(50);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "rating"));
    }

    @Test
    void fallsBackToAscendingForAnUnrecognizedDirection() {
        Pageable pageable = PageSupport.pageable(0, 20, "rating", "sideways", WHITELIST);

        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "rating"));
    }

    @Test
    void rejectsUnknownSortFieldWithBadRequestListingTheSortableFields() {
        assertThatThrownBy(() -> PageSupport.pageable(0, 20, "password", null, WHITELIST))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("password")
                .hasMessageContaining("companyName, registeredAt, rating, status")
                .extracting(exception -> ((BusinessException) exception).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsPageOntoPageResponse() {
        Page<Integer> page = new PageImpl<>(List.of(1, 2, 3), PageRequest.of(1, 3), 10);

        PageResponse<String> response = PageSupport.map(page, String::valueOf);

        assertThat(response.content()).containsExactly("1", "2", "3");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(3);
        assertThat(response.totalElements()).isEqualTo(10);
        assertThat(response.totalPages()).isEqualTo(4);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }
}
