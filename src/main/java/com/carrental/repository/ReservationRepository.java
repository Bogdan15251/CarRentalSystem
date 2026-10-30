package com.carrental.repository;

import com.carrental.domain.CarType;
import com.carrental.domain.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * IDs of cars of the given type that have an ACTIVE reservation overlapping [start, end).
     * Overlap of half-open intervals: existing.start < requested.end AND requested.start < existing.end.
     * The check runs in the database so we never load the whole reservation history into memory.
     */
    @Query("""
            select distinct r.car.id from Reservation r
            where r.car.type = :type
              and r.status = com.carrental.domain.ReservationStatus.ACTIVE
              and r.period.startAt < :end
              and r.period.endAt > :start
            """)
    Set<Long> findBusyCarIds(@Param("type") CarType type,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    @EntityGraph(attributePaths = "car")
    Optional<Reservation> findWithCarById(Long id);
}
