package com.amit.testcontainer.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.core.RedisTemplate;

@TestConfiguration
public class RedisConfiguration {

    @Bean
    @Primary
    public RedisStandaloneConfiguration redisStandaloneConfiguration() {
        return new RedisStandaloneConfiguration(RedisContainer.REDIS_CONTAINER.getHost(), RedisContainer.REDIS_CONTAINER.getMappedPort(6379));
    }

}
