package com.carrental.api.dto;

import com.carrental.domain.CarType;
import com.carrental.domain.RentalPeriod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Bean Validation handles request shape (fail fast, 400 with field errors).
 * Business rules (not in the past, availability) stay in the service/domain,
 * so they hold no matter who calls the service.
 */
public record CreateReservationRequest(
        @NotNull CarType carType,
        @NotNull LocalDateTime startAt,
        @NotNull @Min(1) @Max(RentalPeriod.MAX_DAYS) Integer days) {
}
