package com.carrental;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Freezes "now" so tests about past/future dates are deterministic. */
@TestConfiguration
public class TestClockConfig {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
    }
}
