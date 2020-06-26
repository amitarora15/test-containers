package com.amit.testcontainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TestContainersApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestContainersApplication.class, args);
    }

}
