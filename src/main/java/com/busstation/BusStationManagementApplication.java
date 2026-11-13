package com.busstation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EntityScan(basePackages = "com.busstation.entity")
public class BusStationManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusStationManagementApplication.class, args);
    }
}