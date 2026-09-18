package com.example.campusmarketserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.campusmarketserver.mapper")
public class CampusMarketServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusMarketServerApplication.class, args);
    }

}
