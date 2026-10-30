package com.carrental.api.dto;

import com.carrental.domain.CarType;
import com.carrental.domain.Reservation;
import com.carrental.domain.ReservationStatus;

import java.time.LocalDateTime;

/** API contract is decoupled from the JPA entity: entities never leak out of the controller. */
public record ReservationResponse(
        Long id,
        CarType carType,
        String plateNumber,
        LocalDateTime startAt,
        LocalDateTime endAt,
        ReservationStatus status) {

    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getCar().getType(),
                r.getCar().getPlateNumber(),
                r.getPeriod().startAt(),
                r.getPeriod().endAt(),
                r.getStatus());
    }
}
