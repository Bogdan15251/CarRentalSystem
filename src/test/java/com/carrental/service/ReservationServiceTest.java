package com.carrental.service;

import com.carrental.TestClockConfig;
import com.carrental.domain.CarType;
import com.carrental.domain.Reservation;
import com.carrental.domain.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests against a real (in-memory) database, because the availability rule
 * lives partly in the JPQL overlap query — mocking the repository would test nothing.
 * Test fleet: SEDAN=2, SUV=3, VAN=1. Each test rolls back.
 */
@SpringBootTest
@Import(TestClockConfig.class)
@Transactional
class ReservationServiceTest {

    private static final LocalDateTime START = TestClockConfig.NOW.plusDays(10).withHour(9);

    @Autowired
    ReservationService service;

    @Test
    void reservesCarOfRequestedTypeForRequestedPeriod() {
        Reservation r = service.reserve(CarType.SEDAN, START, 3);

        assertThat(r.getId()).isNotNull();
        assertThat(r.getCar().getType()).isEqualTo(CarType.SEDAN);
        assertThat(r.getPeriod().startAt()).isEqualTo(START);
        assertThat(r.getPeriod().endAt()).isEqualTo(START.plusDays(3));
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void fleetLimitIsEnforcedForOverlappingPeriods() {
        service.reserve(CarType.SEDAN, START, 3);
        service.reserve(CarType.SEDAN, START.plusDays(1), 3);

        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, START.plusDays(2), 1))
                .isInstanceOf(NoCarAvailableException.class);
    }

    @Test
    void eachCarIsAssignedAtMostOnceForOverlappingPeriods() {
        Reservation a = service.reserve(CarType.SEDAN, START, 3);
        Reservation b = service.reserve(CarType.SEDAN, START, 3);

        assertThat(a.getCar().getId()).isNotEqualTo(b.getCar().getId());
    }

    @Test
    void sameCarCanBeRentedBackToBack() {
        Reservation first = service.reserve(CarType.VAN, START, 2);
        Reservation next = service.reserve(CarType.VAN, START.plusDays(2), 2);

        assertThat(next.getCar().getId()).isEqualTo(first.getCar().getId());
    }

    @Test
    void carTypesHaveIndependentLimits() {
        service.reserve(CarType.VAN, START, 3);

        assertThatThrownBy(() -> service.reserve(CarType.VAN, START, 3))
                .isInstanceOf(NoCarAvailableException.class);
        assertThat(service.reserve(CarType.SUV, START, 3)).isNotNull();
    }

    @Test
    void allCarsOfTypeCanBeReservedUpToFleetSize() {
        IntStream.range(0, 3).forEach(i -> service.reserve(CarType.SUV, START, 5));

        assertThat(service.countAvailable(CarType.SUV, START, 5)).isZero();
        assertThatThrownBy(() -> service.reserve(CarType.SUV, START, 5))
                .isInstanceOf(NoCarAvailableException.class);
    }

    @Test
    void cancellationFreesTheCar() {
        Reservation r = service.reserve(CarType.VAN, START, 3);
        service.cancel(r.getId());

        assertThat(service.reserve(CarType.VAN, START, 3)).isNotNull();
    }

    @Test
    void availabilityReflectsExistingReservations() {
        assertThat(service.countAvailable(CarType.SEDAN, START, 1)).isEqualTo(2);

        service.reserve(CarType.SEDAN, START, 3);

        assertThat(service.countAvailable(CarType.SEDAN, START, 1)).isEqualTo(1);
        assertThat(service.countAvailable(CarType.SEDAN, START.plusDays(3), 1)).isEqualTo(2);
    }

    @Test
    void rejectsReservationInThePast() {
        assertThatThrownBy(() -> service.reserve(CarType.SEDAN, TestClockConfig.NOW.minusHours(1), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");
    }

    @Test
    void unknownReservationIsNotFound() {
        assertThatThrownBy(() -> service.get(999_999L))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
