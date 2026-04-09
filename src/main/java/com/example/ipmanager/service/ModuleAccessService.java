package com.example.ipmanager.service;

import com.example.ipmanager.model.AccessLevel;
import com.example.ipmanager.model.AppUser;
import com.example.ipmanager.model.Module;
import com.example.ipmanager.model.UserModuleAccess;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModuleAccessService {

    public List<Module> getVisibleModules(AppUser user) {
        if (user == null || user.getModuleAccesses() == null) {
            return List.of();
        }

        return user.getModuleAccesses().stream()
                .filter(a -> a.getModule() != null)
                .filter(a -> Boolean.TRUE.equals(a.getModule().getEnabled()))
                .filter(a -> a.getAccessLevel() != null && a.getAccessLevel() != AccessLevel.NONE)
                .map(UserModuleAccess::getModule)
                .distinct()
                .sorted(Comparator.comparing(m -> m.getSortOrder() != null ? m.getSortOrder() : 0))
                .collect(Collectors.toList());
    }

    public boolean hasViewAccess(AppUser user, String moduleCode) {
        return hasLevel(user, moduleCode, AccessLevel.VIEWER) || hasLevel(user, moduleCode, AccessLevel.ADMIN);
    }

    public boolean hasAdminAccess(AppUser user, String moduleCode) {
        return hasLevel(user, moduleCode, AccessLevel.ADMIN);
    }

    private boolean hasLevel(AppUser user, String moduleCode, AccessLevel expectedLevel) {
        if (user == null || user.getModuleAccesses() == null || moduleCode == null) {
            return false;
        }

        return user.getModuleAccesses().stream()
                .anyMatch(a ->
                        a.getModule() != null &&
                        a.getModule().getCode() != null &&
                        moduleCode.equalsIgnoreCase(a.getModule().getCode()) &&
                        a.getAccessLevel() == expectedLevel
                );
    }
}