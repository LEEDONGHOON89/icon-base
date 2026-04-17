package com.itmasters.icon.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * ICON Rule Engine Application
 *
 * 실시간 룰 실행 및 모니터링을 담당하는 엔진 서비스
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EntityScan(basePackages = {
    "com.itmasters.icon.engine.adapter.out.persistence.entity",
    "com.itmasters.icon.entity"
})
public class EngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(EngineApplication.class, args);
    }
}
