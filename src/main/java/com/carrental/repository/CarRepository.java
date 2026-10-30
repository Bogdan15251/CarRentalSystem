package com.carrental.repository;

import com.carrental.domain.Car;
import com.carrental.domain.CarType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {

    /**
     * SELECT ... FOR UPDATE on all cars of the type.
     * Serializes concurrent reservations of the same type (different types do not block each other),
     * which closes the check-then-act race between "find a free car" and "insert reservation".
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Car c where c.type = :type order by c.id")
    List<Car> lockAllByType(@Param("type") CarType type);

    long countByType(CarType type);
}
