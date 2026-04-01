package com.meridian.positionmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication
@EnableJms
public class PositionManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PositionManagerApplication.class, args);
    }
}
