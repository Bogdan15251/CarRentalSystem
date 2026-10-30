package com.carrental.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A physical car in the fleet. The fleet size per type is the number of Car rows,
 * so "limited number of cars" is modelled as data, not as a counter that can drift.
 */
@Entity
@Table(name = "car")
public class Car {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarType type;

    @Column(nullable = false, unique = true)
    private String plateNumber;

    protected Car() {
        // for JPA
    }

    public Car(CarType type, String plateNumber) {
        this.type = type;
        this.plateNumber = plateNumber;
    }

    public Long getId() {
        return id;
    }

    public CarType getType() {
        return type;
    }

    public String getPlateNumber() {
        return plateNumber;
    }
}
