package com.example.ipmanager.controller;

import com.example.ipmanager.service.AppUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    private final AppUserService appUserService;

    public AuthController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("users", appUserService.findAll());
        return "login";
    }

    @GetMapping("/debug-page")
    public String debugPage() {
        return "debug-page";
    }
}