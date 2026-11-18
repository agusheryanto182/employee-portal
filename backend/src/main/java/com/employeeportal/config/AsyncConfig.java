package com.employeeportal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables {@code @Async}, which runs on Spring Boot's auto-configured task executor. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
