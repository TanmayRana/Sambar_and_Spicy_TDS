package com.sambarspicy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sambarspicy.controller.PublicMenuController.PublicMenu;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/** Prepared separately from the existing revision-based public-menu cache. */
@Configuration(proxyBeanMethods = false)
@EnableCaching
@ConditionalOnProperty(name = "app.spring-cache.enabled", havingValue = "true")
public class RedisCacheConfiguration {
    public static final String PUBLIC_MENU_SNAPSHOT = "public-menu-snapshot";

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connections, ObjectMapper mapper,
            @Value("${app.public-menu-cache.namespace:sambar}") String namespace,
            @Value("${app.spring-cache.public-menu-ttl:30s}") Duration ttl) {
        if (!namespace.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Cache namespace must contain only letters, digits, underscores or hyphens.");
        }
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Public-menu snapshot TTL must be positive.");
        }
        ObjectMapper safeMapper = mapper.copy().deactivateDefaultTyping();
        var defaults = org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(name -> namespace + ":spring-cache:v1:" + name + "::")
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(safeMapper, PublicMenu.class)));
        var publicMenu = defaults.entryTtl(ttl);
        return RedisCacheManager.builder(connections)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(PUBLIC_MENU_SNAPSHOT, publicMenu))
                .disableCreateOnMissingCache()
                .transactionAware()
                .build();
    }
}
