package com.vendorsphere.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Supplies the application clock.
 *
 * <p>Components that need the current date or instant inject this bean instead of calling
 * {@code Instant.now()} or {@code LocalDate.now()}, so tests can substitute a fixed clock. UTC is
 * used throughout, matching the scheduled jobs.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
