package com.carrental.config;

import com.carrental.domain.CarType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.Map;

/** Fleet size per car type, from application.yml (rental.fleet.SEDAN: 5 ...). */
@ConfigurationProperties(prefix = "rental")
public record FleetProperties(Map<CarType, Integer> fleet) {

    public FleetProperties {
        Map<CarType, Integer> copy = new EnumMap<>(CarType.class);
        if (fleet != null) {
            copy.putAll(fleet);
        }
        fleet = copy;
    }
}
