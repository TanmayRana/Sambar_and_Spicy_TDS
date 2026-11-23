package com.sambarspicy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalEnvConfigurationTest {
    @TempDir Path directory;

    @Test
    void extensionlessLocalFileLoadsDatasourceCredentials() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/application.properties"));
        assertTrue(config.contains("spring.config.import=optional:file:./.env[.properties]"));
        Path envFile = directory.resolve(".env");
        Files.writeString(envFile, "SPRING_DATASOURCE_USERNAME=example_user\nSPRING_DATASOURCE_PASSWORD=example-password\n");
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.config.import", envFile.toUri() + "[.properties]");
        environment.setProperty("spring.datasource.password", "${SPRING_DATASOURCE_PASSWORD:}");
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        assertEquals("example_user", environment.getProperty("SPRING_DATASOURCE_USERNAME"));
        assertEquals("example-password", environment.getProperty("spring.datasource.password"));
    }

    @Test
    void redisEnvironmentValuesBindWithoutChangingUrlCompatibility() throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("REDIS_URL", "rediss://example.invalid:6380")
                .withProperty("REDIS_DATABASE", "3")
                .withProperty("REDIS_CONNECT_TIMEOUT", "750ms")
                .withProperty("REDIS_TIMEOUT", "2s");
        environment.getPropertySources().addLast(new ResourcePropertySource(
                new FileSystemResource("src/main/resources/application.properties")));
        RedisProperties redis = Binder.get(environment).bind("spring.data.redis", RedisProperties.class).get();
        assertEquals("rediss://example.invalid:6380", redis.getUrl());
        assertEquals(3, redis.getDatabase());
        assertEquals(Duration.ofMillis(750), redis.getConnectTimeout());
        assertEquals(Duration.ofSeconds(2), redis.getTimeout());
    }
}
