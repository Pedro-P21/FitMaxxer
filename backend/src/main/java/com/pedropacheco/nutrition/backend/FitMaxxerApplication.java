package com.pedropacheco.nutrition.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FitMaxxerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FitMaxxerApplication.class, args);
    }
}
