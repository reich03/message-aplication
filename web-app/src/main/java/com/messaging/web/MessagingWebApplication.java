package com.messaging.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicación principal de la web MVC
 * Sistema de mensajería con Spring Boot
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MessagingWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagingWebApplication.class, args);
    }
}
