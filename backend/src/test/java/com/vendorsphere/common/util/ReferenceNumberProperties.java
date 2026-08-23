package com.vendorsphere.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.UniqueElements;

/**
 * Property-based checks on the shape and ordering of generated reference numbers.
 *
 * <p>{@link ReferenceNumberFormatter} is the pure half of reference allocation: it turns an already
 * allocated sequence value into the printed reference. That lets the format rules of requirements
 * 1.1, 1.2 and 1.4 and the ordering rule of requirement 1.3 be checked with no database at all. The
 * database half — that concurrent allocations against one {@code (organization, prefix, year)} key
 * really do receive strictly increasing, distinct sequence values — is requirement 1.6 and is
 * covered by the concurrency integration test.
 *
 * <p>The expected shape is transcribed by hand from the acceptance criteria rather than read off the
 * formatter's own pattern constant, so the formatter is not being compared against itself.
 */
class ReferenceNumberProperties {

    /** Requirement 1.1 and 1.2, transcribed: prefix, four-digit year, at least three digits. */
    private static final Pattern REFERENCE =
            Pattern.compile("^(VEN|PR|RFQ|PO|DEL)-\\d{4}-\\d{3,}$");

    /**
     * Years across the whole four-digit range, with the boundaries and the current business year
     * given explicit weight so they appear in every run rather than only by chance.
     */
    @Provide
    Arbitrary<Integer> years() {
        return Arbitraries.frequencyOf(
                Tuple.of(3, Arbitraries.of(0, 1, 999, 1000, 2026, 9999)),
                Tuple.of(
                        7,
                        Arbitraries.integers()
                                .between(
                                        ReferenceNumberFormatter.MIN_YEAR,
                                        ReferenceNumberFormatter.MAX_YEAR)));
    }

    /**
     * Sequence values from 1 upward, weighted towards the padding boundaries so the "grows past
     * three digits rather than truncating" behaviour of requirement 1.4 is exercised on every run.
     */
    @Provide
    Arbitrary<Integer> sequences() {
        return Arbitraries.frequencyOf(
                Tuple.of(4, Arbitraries.of(1, 2, 9, 10, 99, 100, 999, 1000, 1001, 99_999)),
                Tuple.of(
                        6,
                        Arbitraries.integers()
                                .between(ReferenceNumberFormatter.MIN_SEQUENCE, 5_000_000)));
    }

    // Feature: procurement-lifecycle, Property 3: Reference number format and sequence monotonicity
    // Validates: Requirements 1.1, 1.2, 1.4
    @Property(tries = 500)
    void formattedReferenceMatchesTheRequiredShape(
            @ForAll ReferencePrefix prefix,
            @ForAll("years") int year,
            @ForAll("sequences") int sequence) {

        String reference = ReferenceNumberFormatter.format(prefix, year, sequence);

        assertThat(reference)
                .as("reference for %s/%d/%d", prefix, year, sequence)
                .matches(REFERENCE);

        String[] segments = reference.split("-");
        assertThat(segments).as("reference splits into exactly three segments").hasSize(3);

        assertThat(segments[0])
                .as("prefix segment equals the requested prefix")
                .isEqualTo(prefix.name());

        assertThat(segments[1]).as("year segment is four characters").hasSize(4);
        assertThat(Integer.parseInt(segments[1]))
                .as("year segment parses back to the requested year")
                .isEqualTo(year);

        assertThat(segments[2].length())
                .as("numeric segment keeps at least three digits")
                .isGreaterThanOrEqualTo(ReferenceNumberFormatter.MIN_SEQUENCE_DIGITS);
        assertThat(Integer.parseInt(segments[2]))
                .as("numeric segment parses back to the allocated sequence")
                .isEqualTo(sequence);
    }

    // Feature: procurement-lifecycle, Property 3: Reference number format and sequence monotonicity
    // Validates: Requirements 1.2, 1.3
    @Property(tries = 300)
    void increasingAllocationsProduceDistinctIncreasingReferences(
            @ForAll ReferencePrefix prefix,
            @ForAll("years") int year,
            @ForAll @Size(min = 2, max = 25) @UniqueElements List<@IntRange(min = 1, max = 200_000) Integer> rawSequences) {

        List<Integer> allocations = new ArrayList<>(rawSequences);
        allocations.sort(Integer::compareTo);

        List<String> references = new ArrayList<>(allocations.size());
        for (int sequence : allocations) {
            references.add(ReferenceNumberFormatter.format(prefix, year, sequence));
        }

        assertThat(references)
                .as("strictly increasing allocations give pairwise distinct references")
                .doesNotHaveDuplicates();

        List<Integer> parsed = references.stream().map(ReferenceNumberProperties::numericSegment).toList();

        assertThat(parsed)
                .as("numeric segments recover the allocation order")
                .containsExactlyElementsOf(allocations);
        assertThat(parsed).as("numeric segments are strictly increasing").isSorted();
        for (int i = 1; i < parsed.size(); i++) {
            assertThat(parsed.get(i))
                    .as("allocation %d is strictly above its predecessor", i)
                    .isGreaterThan(parsed.get(i - 1));
        }
    }

    // Feature: procurement-lifecycle, Property 3: Reference number format and sequence monotonicity
    // Validates: Requirements 1.2, 1.3
    @Property(tries = 100)
    void firstAllocationOfEveryKeyStartsAtZeroZeroOne(
            @ForAll ReferencePrefix prefix, @ForAll("years") int year) {

        String first =
                ReferenceNumberFormatter.format(prefix, year, ReferenceNumberFormatter.MIN_SEQUENCE);

        assertThat(first).matches(REFERENCE);
        assertThat(first.split("-")[2])
                .as("a fresh (organization, prefix, year) key starts at 001")
                .isEqualTo("001");
    }

    private static int numericSegment(String reference) {
        return Integer.parseInt(reference.split("-")[2]);
    }
}
