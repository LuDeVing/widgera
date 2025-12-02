package com.widgera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class WidgeraApplication {

    public static void main(String[] args) {
        SpringApplication.run(WidgeraApplication.class, args);
    }
}
