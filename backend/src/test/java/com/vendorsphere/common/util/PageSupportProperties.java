package com.vendorsphere.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.vendorsphere.common.exception.BusinessException;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

class PageSupportProperties {

    private static final List<String> FIELD_POOL = List.of(
            "companyName", "registeredAt", "rating", "status",
            "createdAt", "totalAmount", "invoiceNumber", "dueDate");

    private static final List<String> OUTSIDE_FIELDS = List.of(
            "password", "passwordHash", "secret", "organizationId", "1; DROP TABLE vendors");

    // Feature: procurement-lifecycle, Property 15: Pagination parameters are defaulted and clamped
    @Property(tries = 200)
    void paginationParametersAreDefaultedAndClamped(
            @ForAll("requestedPages") Integer page,
            @ForAll("requestedSizes") Integer size,
            @ForAll("sortValues") String sort,
            @ForAll("directionValues") String direction,
            @ForAll("sortWhitelists") SortWhitelist whitelist) {

        int expectedPage = (page == null || page < 0) ? 0 : page;
        int expectedSize = (size == null) ? 20 : Math.min(100, Math.max(1, size));

        // Requirement 31.3 / 31.4 on the pure helpers, independently of the sort outcome.
        assertThat(PageSupport.resolvePage(page)).isEqualTo(expectedPage);
        assertThat(PageSupport.resolveSize(size)).isEqualTo(expectedSize);
        assertThat(expectedSize).isBetween(1, 100);

        String candidate = (sort == null) ? null : sort.trim();
        boolean defaulted = candidate == null || candidate.isEmpty();
        boolean accepted = defaulted || whitelist.permits(candidate);

        Statistics.label("sort outcome").collect(accepted ? "accepted" : "rejected");
        Statistics.label("sort outcome").coverage(coverage -> {
            coverage.check("accepted").percentage(percentage -> percentage > 5.0);
            coverage.check("rejected").percentage(percentage -> percentage > 5.0);
        });

        if (accepted) {
            String expectedField = defaulted ? whitelist.defaultField() : candidate;

            assertThat(PageSupport.resolveSortField(sort, whitelist)).isEqualTo(expectedField);

            Pageable pageable = PageSupport.pageable(page, size, sort, direction, whitelist);

            assertThat(pageable.getPageNumber()).isEqualTo(expectedPage);
            assertThat(pageable.getPageSize()).isEqualTo(expectedSize);

            List<Sort.Order> orders = pageable.getSort().toList();
            assertThat(orders).hasSize(1);
            assertThat(orders.get(0).getProperty()).isEqualTo(expectedField);
            assertThat(orders.get(0).getDirection()).isEqualTo(expectedDirection(direction));

            // Requirement 31.5: nothing outside the whitelist ever reaches the query.
            assertThat(orders)
                    .allSatisfy(order -> assertThat(whitelist.permits(order.getProperty())).isTrue());
        } else {
            // Requirement 31.5: 400 naming every sortable field, from both entry points.
            assertRejected(() -> PageSupport.pageable(page, size, sort, direction, whitelist),
                    candidate, whitelist);
            assertRejected(() -> PageSupport.resolveSortField(sort, whitelist), candidate, whitelist);
        }
    }

    private static void assertRejected(Runnable call, String candidate, SortWhitelist whitelist) {
        Throwable thrown = catchThrowable(call::run);

        assertThat(thrown).isInstanceOf(BusinessException.class);
        BusinessException rejection = (BusinessException) thrown;

        assertThat(rejection.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rejection.getMessage()).contains(candidate);
        assertThat(whitelist.fields())
                .allSatisfy(field -> assertThat(rejection.getMessage()).contains(field));
    }

    private static Sort.Direction expectedDirection(String direction) {
        if (direction == null) {
            return Sort.Direction.ASC;
        }
        return "desc".equalsIgnoreCase(direction.trim()) ? Sort.Direction.DESC : Sort.Direction.ASC;
    }

    @Provide
    Arbitrary<Integer> requestedPages() {
        return Arbitraries.oneOf(
                        Arbitraries.integers().between(-50, 500),
                        Arbitraries.of(0, 1, -1, 100_000, Integer.MAX_VALUE, Integer.MIN_VALUE))
                .injectNull(0.15);
    }

    @Provide
    Arbitrary<Integer> requestedSizes() {
        return Arbitraries.oneOf(
                        Arbitraries.integers().between(-10, 250),
                        Arbitraries.of(0, 1, 20, 100, 101, -1, Integer.MAX_VALUE, Integer.MIN_VALUE))
                .injectNull(0.15);
    }

    @Provide
    Arbitrary<String> sortValues() {
        return Arbitraries.oneOf(
                        Arbitraries.of(FIELD_POOL),
                        Arbitraries.of(FIELD_POOL).map(field -> "  " + field + " "),
                        Arbitraries.of(OUTSIDE_FIELDS),
                        Arbitraries.of("", " ", "\t", "\n"),
                        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(12))
                .injectNull(0.1);
    }

    @Provide
    Arbitrary<String> directionValues() {
        return Arbitraries.oneOf(
                        Arbitraries.of("asc", "ASC", "Asc", "desc", "DESC", "dEsC", " desc ", "asc "),
                        Arbitraries.of("", "  ", "sideways", "1", "ascending", "descending"),
                        Arbitraries.strings().alpha().ofMaxLength(6))
                .injectNull(0.15);
    }

    @Provide
    Arbitrary<SortWhitelist> sortWhitelists() {
        return Arbitraries.of(FIELD_POOL)
                .list().ofMinSize(1).ofMaxSize(5).uniqueElements()
                .map(fields -> SortWhitelist.of(fields, fields.get(0)));
    }
}
