package com.itmasters.icon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
@EntityScan(basePackages = "com.itmasters.icon")
public class IconApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IconApiApplication.class, args);
    }
}
