package com.vendorsphere.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ReferenceNumberFormatterTest {

    @Test
    void padsFirstSequenceToThreeDigits() {
        assertThat(ReferenceNumberFormatter.format(ReferencePrefix.RFQ, 2026, 1))
                .isEqualTo("RFQ-2026-001");
    }

    @Test
    void keepsThreeDigitsUpToNineHundredNinetyNine() {
        assertThat(ReferenceNumberFormatter.format(ReferencePrefix.PO, 2026, 42))
                .isEqualTo("PO-2026-042");
        assertThat(ReferenceNumberFormatter.format(ReferencePrefix.PO, 2026, 999))
                .isEqualTo("PO-2026-999");
    }

    @Test
    void growsBeyondThreeDigitsInsteadOfTruncating() {
        assertThat(ReferenceNumberFormatter.format(ReferencePrefix.DEL, 2026, 1000))
                .isEqualTo("DEL-2026-1000");
    }

    @ParameterizedTest
    @EnumSource(ReferencePrefix.class)
    void usesTheEnumNameAsPrefixSegment(ReferencePrefix prefix) {
        assertThat(ReferenceNumberFormatter.format(prefix, 2026, 7))
                .isEqualTo(prefix.name() + "-2026-007");
    }

    @Test
    void padsYearToFourDigits() {
        assertThat(ReferenceNumberFormatter.format(ReferencePrefix.VEN, 7, 1))
                .isEqualTo("VEN-0007-001");
    }

    @Test
    void rejectsSequenceBelowOne() {
        assertThatThrownBy(() -> ReferenceNumberFormatter.format(ReferencePrefix.VEN, 2026, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sequence must be at least 1");
    }

    @Test
    void rejectsYearOutsideFourDigits() {
        assertThatThrownBy(() -> ReferenceNumberFormatter.format(ReferencePrefix.VEN, 10_000, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("year must be between");
    }

    @Test
    void rejectsNullPrefix() {
        assertThatThrownBy(() -> ReferenceNumberFormatter.format(null, 2026, 1))
                .isInstanceOf(NullPointerException.class);
    }
}
