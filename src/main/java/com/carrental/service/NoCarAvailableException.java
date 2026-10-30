package com.carrental.service;

import com.carrental.domain.CarType;
import com.carrental.domain.RentalPeriod;

public class NoCarAvailableException extends RuntimeException {

    public NoCarAvailableException(CarType type, RentalPeriod period) {
        super("No %s available from %s to %s".formatted(type, period.startAt(), period.endAt()));
    }
}
