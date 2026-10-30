package com.carrental.config;

import com.carrental.domain.Car;
import com.carrental.repository.CarRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds the simulated fleet on startup (only if the table is empty). */
@Component
public class FleetInitializer implements ApplicationRunner {

    private final CarRepository carRepository;
    private final FleetProperties fleetProperties;

    public FleetInitializer(CarRepository carRepository, FleetProperties fleetProperties) {
        this.carRepository = carRepository;
        this.fleetProperties = fleetProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (carRepository.count() > 0) {
            return;
        }
        fleetProperties.fleet().forEach((type, count) -> {
            for (int i = 1; i <= count; i++) {
                carRepository.save(new Car(type, "%s-%03d".formatted(type, i)));
            }
        });
    }
}
