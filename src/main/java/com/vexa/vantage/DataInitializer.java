package com.vexa.vantage;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        // Initializer simplified
        System.out.println("⚡ Backend Application Started");
    }
}