package com.example.ipmanager.controller;

import com.example.ipmanager.service.IpService;
import com.example.ipmanager.util.InputSecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/validate")
public class ValidationController {

    private final IpService ipService;

    public ValidationController(IpService ipService) {
        this.ipService = ipService;
    }

    @PostMapping("/enlace")
    public Map<String, Object> validateEnlace(@RequestBody Map<String, String> body) {
        String enlace = InputSecurityUtil.sanitizeEnlace(body.get("enlace"));
        Long id = parseLong(body.get("id"));
        return ipService.validateEnlace(enlace, id);
    }

    @PostMapping("/lan")
    public Map<String, Object> validateLan(@RequestBody Map<String, String> body) {
        String ip = InputSecurityUtil.sanitizeIp(body.get("ip"), "LAN IP");
        Integer cidr = parseInteger(body.get("cidr"));
        Long id = parseLong(body.get("id"));
        return ipService.validateLan(ip, cidr, id);
    }

    @PostMapping("/wan")
    public Map<String, Object> validateWan(@RequestBody Map<String, String> body) {
        String ip = InputSecurityUtil.sanitizeIp(body.get("ip"), "WAN IP");
        Integer cidr = parseInteger(body.get("cidr"));
        Long id = parseLong(body.get("id"));
        return ipService.validateWan(ip, cidr, id);
    }

    @GetMapping("/siguiente-ip")
    public Map<String, Object> obtenerSiguienteIp(@RequestParam("tipo") String tipo) {
        return Map.of("ip", ipService.obtenerSiguienteIpDisponible(tipo));
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value.trim());
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value.trim());
    }
}