package com.example.ipmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IpManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(IpManagerApplication.class, args);
    }
}

/*package com.example.ipmanager;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class IpManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IpManagerApplication.class, args);
    }

    @Bean
    public CommandLineRunner generarHash() {
        return args -> {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String rawPassword = "Admin123";
            String hash = encoder.encode(rawPassword);
            System.out.println("HASH REAL: " + hash);
        };
    }
}*/