package com.example.ipmanager.config;

import com.example.ipmanager.model.AppUser;
import com.example.ipmanager.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!appUserRepository.existsByUsername("admin")) {
                  /*AppUser admin = new AppUser(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "ADMIN",
                    true,
                    "Administrador",
                    "Sistema",
                    "",
                    "admin@ccss.local"
            );
            appUserRepository.save(admin);*/
        }
    }
}