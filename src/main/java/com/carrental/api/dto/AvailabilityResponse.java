package com.carrental.api.dto;

import com.carrental.domain.CarType;

import java.time.LocalDateTime;

public record AvailabilityResponse(CarType carType, LocalDateTime startAt, int days, long available) {
}
