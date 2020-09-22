package com.changhong.sei;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class EdmApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdmApplication.class, args);
    }

}
