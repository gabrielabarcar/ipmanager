package com.example.ipmanager.config;

import com.example.ipmanager.model.AppUser;
import com.example.ipmanager.service.CurrentUserService;
import com.example.ipmanager.service.ModuleAccessService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final CurrentUserService currentUserService;
    private final ModuleAccessService moduleAccessService;

    public GlobalModelAttributes(CurrentUserService currentUserService,
                                 ModuleAccessService moduleAccessService) {
        this.currentUserService = currentUserService;
        this.moduleAccessService = moduleAccessService;
    }

    @ModelAttribute("currentAppUser")
    public AppUser currentAppUser() {
        return currentUserService.getCurrentAppUser();
    }

    @ModelAttribute("allowedModules")
    public Object allowedModules() {
        AppUser user = currentUserService.getCurrentAppUser();
        if (user == null) {
            return java.util.List.of();
        }
        return moduleAccessService.getVisibleModules(user);
    }
}