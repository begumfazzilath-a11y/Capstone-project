package com.fazzimart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FazziMartApplication {

    public static void main(String[] args) {
        SpringApplication.run(FazziMartApplication.class, args);
        System.out.println("==============================================");
        System.out.println("  FAZZI MART backend is running!");
        System.out.println("  Swagger/API base: http://localhost:8080/api");
        System.out.println("==============================================");
    }
}