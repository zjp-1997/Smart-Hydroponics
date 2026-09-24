package com.smart_plant.smart_plant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@MapperScan("com.smart_plant.smart_plant.mapper")
public class SmartPlantApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartPlantApplication.class, args);
    }

}
