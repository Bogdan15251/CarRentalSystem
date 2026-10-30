package com.carrental.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A reservation binds a concrete car to a rental period.
 *
 * Assigning a specific car (rather than only decrementing a per-type counter) makes
 * correctness easy to reason about: "no car has two overlapping ACTIVE reservations".
 */
@Entity
@Table(name = "reservation",
        indexes = @Index(name = "ix_reservation_car_period", columnList = "car_id, start_at, end_at"))
public class Reservation {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Embedded
    private RentalPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    protected Reservation() {
        // for JPA
    }

    public Reservation(Car car, RentalPeriod period) {
        this.car = car;
        this.period = period;
        this.status = ReservationStatus.ACTIVE;
    }

    /** Idempotent: cancelling twice is not an error. */
    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public Car getCar() {
        return car;
    }

    public RentalPeriod getPeriod() {
        return period;
    }

    public ReservationStatus getStatus() {
        return status;
    }
}
