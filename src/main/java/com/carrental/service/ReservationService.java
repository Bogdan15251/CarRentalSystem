package com.carrental.service;

import com.carrental.domain.Car;
import com.carrental.domain.CarType;
import com.carrental.domain.RentalPeriod;
import com.carrental.domain.Reservation;
import com.carrental.repository.CarRepository;
import com.carrental.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ReservationService {

    private final CarRepository carRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public ReservationService(CarRepository carRepository,
                              ReservationRepository reservationRepository,
                              Clock clock) {
        this.carRepository = carRepository;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    /**
     * Reserves a car of the given type.
     *
     * Concurrency: the naive "check free cars, then insert" is a race — two requests can both
     * see the last free car. We lock the cars of this type first (pessimistic lock), so the
     * check and the insert are atomic with respect to other reservations of the same type.
     */
    @Transactional
    public Reservation reserve(CarType type, LocalDateTime startAt, int days) {
        RentalPeriod period = RentalPeriod.of(startAt, days);
        if (period.startsBefore(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("Reservation cannot start in the past");
        }

        List<Car> cars = carRepository.lockAllByType(type);
        Set<Long> busy = reservationRepository.findBusyCarIds(type, period.startAt(), period.endAt());

        Car freeCar = cars.stream()
                .filter(car -> !busy.contains(car.getId()))
                .findFirst()
                .orElseThrow(() -> new NoCarAvailableException(type, period));

        return reservationRepository.save(new Reservation(freeCar, period));
    }

    @Transactional(readOnly = true)
    public long countAvailable(CarType type, LocalDateTime startAt, int days) {
        RentalPeriod period = RentalPeriod.of(startAt, days);
        long total = carRepository.countByType(type);
        long busy = reservationRepository.findBusyCarIds(type, period.startAt(), period.endAt()).size();
        return total - busy;
    }

    @Transactional(readOnly = true)
    public Reservation get(Long id) {
        return reservationRepository.findWithCarById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    @Transactional
    public Reservation cancel(Long id) {
        Reservation reservation = get(id);
        reservation.cancel();
        return reservation;
    }
}
