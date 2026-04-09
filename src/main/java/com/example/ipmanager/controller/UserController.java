package com.example.ipmanager.controller;

import com.example.ipmanager.model.AccessLevel;
import com.example.ipmanager.model.AppUser;
import com.example.ipmanager.model.Module;
import com.example.ipmanager.model.UserModuleAccess;
import com.example.ipmanager.repository.ModuleRepository;
import com.example.ipmanager.security.AuthenticatedUser;
import com.example.ipmanager.service.AppUserService;
import com.example.ipmanager.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/users")
public class UserController {

    private final AppUserService appUserService;
    private final SessionRegistry sessionRegistry;
    private final CustomUserDetailsService customUserDetailsService;
    private final ModuleRepository moduleRepository;

    public UserController(AppUserService appUserService,
                          SessionRegistry sessionRegistry,
                          CustomUserDetailsService customUserDetailsService,
                          ModuleRepository moduleRepository) {
        this.appUserService = appUserService;
        this.sessionRegistry = sessionRegistry;
        this.customUserDetailsService = customUserDetailsService;
        this.moduleRepository = moduleRepository;
    }

    @GetMapping
    public String users(Model model) {
        AppUser appUser = new AppUser();
        appUser.setRole("VIEWER");

        model.addAttribute("users", appUserService.findAll());
        model.addAttribute("appUser", appUser);
        model.addAttribute("authenticatedUsernames", authenticatedUsernames());
        model.addAttribute("modules", moduleRepository.findByEnabledTrueOrderBySortOrderAscNameAsc());
        model.addAttribute("moduleAccessMap", new HashMap<Long, String>());

        return "users";
    }

    @GetMapping("/edit/{id}")
    public String editUser(@PathVariable Long id, Model model) {
        AppUser appUser = appUserService.findById(id);

        appUser.setPassword("");
        if (appUser.getRole() == null || appUser.getRole().isBlank()) {
            appUser.setRole("VIEWER");
        }

        Map<Long, String> moduleAccessMap = new HashMap<>();
        if (appUser.getModuleAccesses() != null) {
            for (UserModuleAccess access : appUser.getModuleAccesses()) {
                if (access.getModule() != null && access.getModule().getId() != null && access.getAccessLevel() != null) {
                    moduleAccessMap.put(access.getModule().getId(), access.getAccessLevel().name());
                }
            }
        }

        model.addAttribute("users", appUserService.findAll());
        model.addAttribute("appUser", appUser);
        model.addAttribute("authenticatedUsernames", authenticatedUsernames());
        model.addAttribute("modules", moduleRepository.findByEnabledTrueOrderBySortOrderAscNameAsc());
        model.addAttribute("moduleAccessMap", moduleAccessMap);

        return "users";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("appUser") AppUser appUser,
                       @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                       @RequestParam Map<String, String> allParams,
                       Authentication authentication,
                       HttpServletRequest request,
                       RedirectAttributes redirectAttributes) {
        try {
            boolean isCreate = appUser.getId() == null;
            boolean changingPassword = appUser.getPassword() != null && !appUser.getPassword().isBlank();

            String derivedRole = deriveLegacyRoleFromModules(allParams);
            appUser.setRole(derivedRole);
            appUser.setModuleAccesses(buildModuleAccessesFromParams(allParams));

            if (isCreate) {
                if (!changingPassword) {
                    throw new RuntimeException("La contraseña es obligatoria.");
                }
                if (!appUser.getPassword().equals(confirmPassword)) {
                    throw new RuntimeException("Las contraseñas no coinciden.");
                }
            } else {
                if (changingPassword && !appUser.getPassword().equals(confirmPassword)) {
                    throw new RuntimeException("Las contraseñas no coinciden.");
                }
            }

            AppUser savedUser = appUserService.save(appUser, changingPassword || isCreate);
            refrescarSesionSiEsElMismoUsuario(savedUser, authentication, request);

            redirectAttributes.addFlashAttribute("success", "Usuario guardado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/users";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("id") Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser currentUser) {
                if (currentUser.getId() != null && currentUser.getId().equals(id)) {
                    throw new RuntimeException("No puede eliminar el usuario autenticado.");
                }
            }

            appUserService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Usuario eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/users";
    }

    private String deriveLegacyRoleFromModules(Map<String, String> allParams) {
        boolean anyAdmin = allParams.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().contains(".accessLevel"))
                .anyMatch(e -> "ADMIN".equalsIgnoreCase(e.getValue()));

        return anyAdmin ? "ADMIN" : "VIEWER";
    }

    private List<UserModuleAccess> buildModuleAccessesFromParams(Map<String, String> allParams) {
        List<UserModuleAccess> result = new ArrayList<>();
        TreeSet<Integer> indexes = new TreeSet<>();

        Pattern pattern = Pattern.compile("moduleAccesses\\[(\\d+)]\\.accessLevel");

        for (String key : allParams.keySet()) {
            Matcher matcher = pattern.matcher(key);
            if (matcher.matches()) {
                indexes.add(Integer.parseInt(matcher.group(1)));
            }
        }

        for (Integer idx : indexes) {
            String moduleIdKey = "moduleAccesses[" + idx + "].module.id";
            String accessKey = "moduleAccesses[" + idx + "].accessLevel";

            String moduleIdValue = allParams.get(moduleIdKey);
            String accessValue = allParams.get(accessKey);

            if (moduleIdValue == null || moduleIdValue.isBlank()) {
                continue;
            }

            if (accessValue == null || accessValue.isBlank()) {
                continue;
            }

            AccessLevel accessLevel = AccessLevel.valueOf(accessValue);

            if (accessLevel == AccessLevel.NONE) {
                continue;
            }

            Long moduleId = Long.parseLong(moduleIdValue);

            Module module = moduleRepository.findById(moduleId)
                    .orElseThrow(() -> new RuntimeException("Módulo no encontrado: " + moduleId));

            UserModuleAccess access = new UserModuleAccess();
            access.setModule(module);
            access.setAccessLevel(accessLevel);

            result.add(access);
        }

        return result;
    }

    private void refrescarSesionSiEsElMismoUsuario(AppUser savedUser,
                                                   Authentication authentication,
                                                   HttpServletRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser currentUser)) {
            return;
        }

        if (savedUser.getId() == null || currentUser.getId() == null) {
            return;
        }

        if (!savedUser.getId().equals(currentUser.getId())) {
            return;
        }

        AuthenticatedUser updatedPrincipal =
                (AuthenticatedUser) customUserDetailsService.loadUserByUsername(savedUser.getUsername());

        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(
                        updatedPrincipal,
                        authentication.getCredentials(),
                        updatedPrincipal.getAuthorities()
                );

        newAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

    private HashSet<String> authenticatedUsernames() {
        HashSet<String> names = new HashSet<>();

        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof AuthenticatedUser user) {
                for (SessionInformation sessionInfo : sessionRegistry.getAllSessions(principal, false)) {
                    if (!sessionInfo.isExpired()) {
                        names.add(user.getUsername());
                    }
                }
            }
        }

        return names;
    }
}