package com.travel.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.travel.platform.mapper")
public class TravelDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelDataApplication.class, args);
    }
}