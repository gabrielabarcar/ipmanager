package com.example.ipmanager.controller;

import com.example.ipmanager.service.AuditLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/logs")
    public String logs(Model model) {

        model.addAttribute("logs", auditLogService.findLatest());

        return "logs";
    }
}