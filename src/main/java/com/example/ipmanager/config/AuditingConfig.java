package com.example.ipmanager.config;

import com.example.ipmanager.security.AuthenticatedUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("SISTEMA");
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof AuthenticatedUser user) {
                return Optional.of(user.getUsername());
            }

            if (principal instanceof String username) {
                return Optional.of(username);
            }

            return Optional.of("SISTEMA");
        };
    }
}