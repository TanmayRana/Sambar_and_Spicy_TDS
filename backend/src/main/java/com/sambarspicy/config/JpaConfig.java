package com.sambarspicy.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import java.util.TimeZone;

/**
 * JPA and transaction management configuration.
 * Enforces Asia/Kolkata JVM timezone default for store-local date/time calculations.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.sambarspicy.repository")
public class JpaConfig {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }
}
