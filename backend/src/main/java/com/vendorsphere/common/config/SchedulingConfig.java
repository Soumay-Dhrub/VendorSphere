package com.vendorsphere.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables the platform's scheduled jobs. The jobs themselves live in the modules that own their data;
 * this configuration only switches the scheduler on, exactly as the design table of daily and
 * intraday jobs describes.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
