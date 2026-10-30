package com.carrental.service;

import com.carrental.TestClockConfig;
import com.carrental.domain.CarType;
import com.carrental.repository.ReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the fleet limit holds under concurrent requests (no overbooking).
 * Without the pessimistic lock in ReservationService this test fails intermittently:
 * several threads see the same "free" car and all insert a reservation.
 *
 * Not @Transactional on purpose: each thread must run its own real transaction.
 */
@SpringBootTest
@Import(TestClockConfig.class)
class ConcurrentReservationTest {

    private static final int SUV_FLEET = 3;
    private static final int THREADS = 20;
    private static final LocalDateTime START = TestClockConfig.NOW.plusDays(30);

    @Autowired
    ReservationService service;

    @Autowired
    ReservationRepository reservationRepository;

    @AfterEach
    void cleanUp() {
        reservationRepository.deleteAll();
    }

    @RepeatedTest(5)
    void concurrentRequestsNeverExceedFleetSize() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            startGate.await();
            try {
                service.reserve(CarType.SUV, START, 2);
                return true;
            } catch (NoCarAvailableException e) {
                return false;
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                results.add(pool.submit(attempt));
            }
            startGate.countDown(); // release all threads at once to maximise contention

            long succeeded = 0;
            for (Future<Boolean> f : results) {
                if (f.get(30, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }

            assertThat(succeeded).isEqualTo(SUV_FLEET);
            assertThat(reservationRepository.count()).isEqualTo(SUV_FLEET);
            assertThat(reservationRepository.findAll())
                    .extracting(r -> r.getCar().getId())
                    .doesNotHaveDuplicates();
        } finally {
            pool.shutdownNow();
        }
    }
}
