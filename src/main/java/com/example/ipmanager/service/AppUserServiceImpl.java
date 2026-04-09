package com.example.ipmanager.service;

import com.example.ipmanager.model.AccessLevel;
import com.example.ipmanager.model.AppUser;
import com.example.ipmanager.model.Module;
import com.example.ipmanager.model.UserModuleAccess;
import com.example.ipmanager.repository.AppUserRepository;
import com.example.ipmanager.repository.ModuleRepository;
import com.example.ipmanager.util.InputSecurityUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepository appUserRepository;
    private final ModuleRepository moduleRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserServiceImpl(AppUserRepository appUserRepository,
                              ModuleRepository moduleRepository,
                              PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.moduleRepository = moduleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<AppUser> findAll() {
        return appUserRepository.findAllByOrderByUsernameAsc();
    }

    @Override
    public AppUser save(AppUser user, boolean encodePassword) {
        sanitize(user);
        validar(user);

        AppUser entity;

        if (user.getId() != null) {
            entity = findById(user.getId());
        } else {
            entity = new AppUser();
        }

        entity.setUsername(user.getUsername());
        entity.setNombre(user.getNombre());
        entity.setApellido1(user.getApellido1());
        entity.setApellido2(user.getApellido2());
        entity.setCorreo(user.getCorreo());
        entity.setEnabled(user.getEnabled() != null ? user.getEnabled() : true);
        entity.setRole(user.getRole());

        if (encodePassword && user.getPassword() != null && !user.getPassword().isBlank()) {
            entity.setPassword(passwordEncoder.encode(user.getPassword()));
        } else if (entity.getId() == null) {
            entity.setPassword(user.getPassword());
        }

        // Limpiar accesos actuales y reconstruir desde el formulario
        entity.getModuleAccesses().clear();

        if (user.getModuleAccesses() != null) {
            for (UserModuleAccess incoming : user.getModuleAccesses()) {
                if (incoming == null || incoming.getAccessLevel() == null) {
                    continue;
                }

                if (incoming.getAccessLevel() == AccessLevel.NONE) {
                    continue;
                }

                if (incoming.getModule() == null || incoming.getModule().getId() == null) {
                    continue;
                }

                Long moduleId = incoming.getModule().getId();

                Module managedModule = moduleRepository.findById(moduleId)
                        .orElseThrow(() -> new RuntimeException("Módulo no encontrado: " + moduleId));

                UserModuleAccess newAccess = new UserModuleAccess();
                newAccess.setUser(entity);
                newAccess.setModule(managedModule);
                newAccess.setAccessLevel(incoming.getAccessLevel());

                entity.getModuleAccesses().add(newAccess);
            }
        }

        return appUserRepository.save(entity);
    }

    @Override
    public AppUser findById(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }

    @Override
    public void deleteById(Long id) {
        appUserRepository.deleteById(id);
    }

    @Override
    public boolean usernameExists(String username, Long currentId) {
        if (username == null || username.isBlank()) {
            return false;
        }

        if (currentId == null) {
            return appUserRepository.existsByUsername(username.trim());
        }

        return appUserRepository.existsByUsernameAndIdNot(username.trim(), currentId);
    }

    @Override
    public boolean correoExists(String correo, Long currentId) {
        if (correo == null || correo.isBlank()) {
            return false;
        }

        if (currentId == null) {
            return appUserRepository.existsByCorreo(correo.trim());
        }

        return appUserRepository.existsByCorreoAndIdNot(correo.trim(), currentId);
    }

    private void sanitize(AppUser user) {
        user.setUsername(InputSecurityUtil.sanitizeUsername(user.getUsername()));
        user.setNombre(InputSecurityUtil.sanitizeGeneralText(user.getNombre(), "Nombre"));
        user.setApellido1(InputSecurityUtil.sanitizeGeneralText(user.getApellido1(), "Apellido 1"));
        user.setApellido2(InputSecurityUtil.sanitizeGeneralText(user.getApellido2(), "Apellido 2"));
        user.setCorreo(InputSecurityUtil.sanitizeEmail(user.getCorreo()));
    }

    private void validar(AppUser user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new RuntimeException("El usuario es obligatorio.");
        }

        if (user.getNombre() == null || user.getNombre().isBlank()) {
            throw new RuntimeException("El nombre es obligatorio.");
        }

        if (user.getApellido1() == null || user.getApellido1().isBlank()) {
            throw new RuntimeException("El primer apellido es obligatorio.");
        }

        if (user.getCorreo() == null || user.getCorreo().isBlank()) {
            throw new RuntimeException("El correo es obligatorio.");
        }

        if (!"ADMIN".equals(user.getRole()) && !"VIEWER".equals(user.getRole())) {
            throw new RuntimeException("El rol debe ser ADMIN o VIEWER.");
        }

        if (usernameExists(user.getUsername(), user.getId())) {
            throw new RuntimeException("Ese nombre de usuario ya existe.");
        }

        if (correoExists(user.getCorreo(), user.getId())) {
            throw new RuntimeException("Ese correo ya existe.");
        }

        if (user.getId() == null) {
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                throw new RuntimeException("La contraseña es obligatoria.");
            }
        }
    }

    public String getCurrentUsername() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return "SISTEMA";
        }
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}