package com.carddemo.config;

import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch configuration.
 * Batch jobs are disabled by default (spring.batch.job.enabled=false).
 * The statement generation is invoked directly via StatementService/StatementController.
 */
@Configuration
public class BatchConfig {
    // Batch jobs disabled on startup; statements generated via REST API
}
