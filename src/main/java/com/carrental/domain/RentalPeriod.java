package com.carrental.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object: a half-open time interval [startAt, endAt).
 *
 * Half-open is deliberate: a car returned at 10:00 can be picked up by the next
 * customer at 10:00 — back-to-back rentals do not conflict.
 *
 * Invariants are enforced in the constructor, so an invalid period cannot exist.
 */
@Embeddable
public record RentalPeriod(
        @Column(name = "start_at", nullable = false) LocalDateTime startAt,
        @Column(name = "end_at", nullable = false) LocalDateTime endAt) {

    public static final int MAX_DAYS = 30;

    public RentalPeriod {
        Objects.requireNonNull(startAt, "startAt");
        Objects.requireNonNull(endAt, "endAt");
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
    }

    public static RentalPeriod of(LocalDateTime startAt, int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new IllegalArgumentException("days must be between 1 and " + MAX_DAYS + ", was " + days);
        }
        return new RentalPeriod(startAt, startAt.plusDays(days));
    }

    public boolean overlaps(RentalPeriod other) {
        return startAt.isBefore(other.endAt) && other.startAt.isBefore(endAt);
    }

    public boolean startsBefore(LocalDateTime moment) {
        return startAt.isBefore(moment);
    }
}
