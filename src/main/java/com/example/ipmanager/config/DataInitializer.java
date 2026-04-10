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

    /*@Override
    public void run(String... args) {
        if (!appUserRepository.existsByUsername("admin")) {
                  AppUser admin = new AppUser(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "ADMIN",
                    true,
                    "Administrador",
                    "Sistema",
                    "",
                    "admin@ccss.local"
            );
            appUserRepository.save(admin);
        }
    }
}*/

/*package com.example.ipmanager.config;

import com.example.ipmanager.model.AccessLevel;
import com.example.ipmanager.model.AppUser;
import com.example.ipmanager.model.Module;
import com.example.ipmanager.model.UserModuleAccess;
import com.example.ipmanager.repository.AppUserRepository;
import com.example.ipmanager.repository.ModuleRepository;
import com.example.ipmanager.repository.UserModuleAccessRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.example.ipmanager.model.AccessLevel;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final ModuleRepository moduleRepository;
    private final UserModuleAccessRepository userModuleAccessRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            AppUserRepository appUserRepository,
            ModuleRepository moduleRepository,
            UserModuleAccessRepository userModuleAccessRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.moduleRepository = moduleRepository;
        this.userModuleAccessRepository = userModuleAccessRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!appUserRepository.existsByUsername("admin")) {
            AppUser admin = new AppUser(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    "ADMIN",
                    true,
                    "Administrador",
                    "Sistema",
                    "",
                    "admin@ccss.local"
            );

            admin.setCreatedAt(LocalDateTime.now());
            admin.setCreatedBy("system");
            admin.setUpdatedAt(LocalDateTime.now());
            admin.setUpdatedBy("system");

            admin = appUserRepository.save(admin);

            List<Module> modules = moduleRepository.findAll();
            for (Module module : modules) {
                UserModuleAccess access = new UserModuleAccess();
                access.setUser(admin);
                access.setModule(module);
                access.setAccessLevel(AccessLevel.ADMIN);
                userModuleAccessRepository.save(access);
            }
        }
    }
}*/