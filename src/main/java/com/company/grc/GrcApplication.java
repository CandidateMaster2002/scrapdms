package com.company.grc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GrcApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrcApplication.class, args);
    }
}
