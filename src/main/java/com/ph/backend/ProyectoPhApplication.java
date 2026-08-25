package com.ph.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class ProyectoPhApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProyectoPhApplication.class, args);
    }
}
// Trigger restart to load Optional import in AuthService
