package com.carrental.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit tests, no Spring: the overlap rule is the heart of the system. */
class RentalPeriodTest {

    private static final LocalDateTime T = LocalDateTime.of(2030, 5, 10, 10, 0);

    @Test
    void endIsStartPlusDays() {
        RentalPeriod p = RentalPeriod.of(T, 3);
        assertThat(p.endAt()).isEqualTo(T.plusDays(3));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, RentalPeriod.MAX_DAYS + 1})
    void rejectsInvalidDays(int days) {
        assertThatThrownBy(() -> RentalPeriod.of(T, days))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void identicalPeriodsOverlap() {
        assertThat(RentalPeriod.of(T, 2).overlaps(RentalPeriod.of(T, 2))).isTrue();
    }

    @Test
    void partialOverlapIsDetectedInBothDirections() {
        RentalPeriod a = RentalPeriod.of(T, 3);
        RentalPeriod b = RentalPeriod.of(T.plusDays(2), 3);
        assertThat(a.overlaps(b)).isTrue();
        assertThat(b.overlaps(a)).isTrue();
    }

    @Test
    void containedPeriodOverlaps() {
        RentalPeriod outer = RentalPeriod.of(T, 10);
        RentalPeriod inner = RentalPeriod.of(T.plusDays(3), 1);
        assertThat(outer.overlaps(inner)).isTrue();
        assertThat(inner.overlaps(outer)).isTrue();
    }

    @Test
    void backToBackPeriodsDoNotOverlap() {
        RentalPeriod first = RentalPeriod.of(T, 2);
        RentalPeriod next = RentalPeriod.of(T.plusDays(2), 2);
        assertThat(first.overlaps(next)).isFalse();
        assertThat(next.overlaps(first)).isFalse();
    }

    @Test
    void oneMinuteIntoPreviousRentalOverlaps() {
        RentalPeriod first = RentalPeriod.of(T, 2);
        RentalPeriod next = RentalPeriod.of(T.plusDays(2).minusMinutes(1), 1);
        assertThat(first.overlaps(next)).isTrue();
    }
}
