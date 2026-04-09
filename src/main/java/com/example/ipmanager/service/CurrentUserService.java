package com.example.ipmanager.service;

import com.example.ipmanager.model.AppUser;
import com.example.ipmanager.repository.AppUserRepository;
import com.example.ipmanager.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return "sistema";
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.getUsername();
        }

        if (authentication.isAuthenticated() && authentication.getName() != null) {
            return authentication.getName();
        }

        return "sistema";
    }

    public AppUser getCurrentAppUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedUser authenticatedUser) {
            Optional<AppUser> userOpt =
                    appUserRepository.findWithModuleAccessesByUsername(authenticatedUser.getUsername());
            return userOpt.orElse(null);
        }

        if (authentication.isAuthenticated() && authentication.getName() != null) {
            Optional<AppUser> userOpt =
                    appUserRepository.findWithModuleAccessesByUsername(authentication.getName());
            return userOpt.orElse(null);
        }

        return null;
    }
}