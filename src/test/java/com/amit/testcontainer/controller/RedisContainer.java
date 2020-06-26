package com.amit.testcontainer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;

import java.util.Arrays;


public class RedisContainer {

    private static final  Logger log = LoggerFactory.getLogger(RedisContainer.class);

   @Container
   public static GenericContainer REDIS_CONTAINER = new GenericContainer("redis:6.0.5");

   static {
       //REDIS_CONTAINER.setPortBindings(Arrays.asList("6379:6379"));
       REDIS_CONTAINER.addExposedPort(6379);
   }



}
