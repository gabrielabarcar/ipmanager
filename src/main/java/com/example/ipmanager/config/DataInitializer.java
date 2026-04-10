package com.example.ipmanager.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // Inicialización deshabilitada.
        // Los usuarios se manejan desde la base de datos.
    }
}