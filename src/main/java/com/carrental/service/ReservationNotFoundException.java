package com.carrental.service;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(Long id) {
        super("Reservation " + id + " not found");
    }
}
