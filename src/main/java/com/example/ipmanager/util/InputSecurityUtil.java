package com.example.ipmanager.util;

import java.util.regex.Pattern;

public final class InputSecurityUtil {

    private static final Pattern SAFE_IP = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern SAFE_ENLACE = Pattern.compile("^\\d{0,8}$");
    private static final Pattern SAFE_USERNAME = Pattern.compile("^[a-zA-Z0-9._@-]{1,100}$");
    private static final Pattern SAFE_EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private InputSecurityUtil() {
    }

    public static String sanitizeGeneralText(String value, String fieldName) {
        if (value == null) {
            return null;
        }

        String sanitized = value.trim();

        if (containsSuspiciousContent(sanitized)) {
            throw new RuntimeException("El campo " + fieldName + " contiene contenido no permitido.");
        }

        return sanitized;
    }

    public static String sanitizeUsername(String value) {
        if (value == null) {
            return null;
        }

        String sanitized = value.trim();

        if (!SAFE_USERNAME.matcher(sanitized).matches()) {
            throw new RuntimeException("El usuario contiene caracteres no permitidos.");
        }

        if (containsSuspiciousContent(sanitized)) {
            throw new RuntimeException("El usuario contiene contenido no permitido.");
        }

        return sanitized;
    }

    public static String sanitizeEmail(String value) {
        if (value == null) {
            return null;
        }

        String sanitized = value.trim();

        if (!SAFE_EMAIL.matcher(sanitized).matches()) {
            throw new RuntimeException("El correo tiene un formato inválido.");
        }

        if (containsSuspiciousContent(sanitized)) {
            throw new RuntimeException("El correo contiene contenido no permitido.");
        }

        return sanitized;
    }

    public static String sanitizeIp(String value, String fieldName) {
        if (value == null) {
            return null;
        }

        String sanitized = value.trim();

        if (!SAFE_IP.matcher(sanitized).matches()) {
            throw new RuntimeException("El campo " + fieldName + " tiene un formato de IP inválido.");
        }

        if (containsSuspiciousContent(sanitized)) {
            throw new RuntimeException("El campo " + fieldName + " contiene contenido no permitido.");
        }

        return sanitized;
    }

    public static String sanitizeEnlace(String value) {
        if (value == null) {
            return null;
        }

        String sanitized = value.trim();

        if (!SAFE_ENLACE.matcher(sanitized).matches()) {
            throw new RuntimeException("El enlace contiene caracteres no permitidos.");
        }

        if (containsSuspiciousContent(sanitized)) {
            throw new RuntimeException("El enlace contiene contenido no permitido.");
        }

        return sanitized;
    }

    public static void validateCidr(Integer cidr, String fieldName) {
        if (cidr == null || cidr < 0 || cidr > 32) {
            throw new RuntimeException("El campo " + fieldName + " debe estar entre 0 y 32.");
        }
    }

    private static boolean containsSuspiciousContent(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.toLowerCase();

        return normalized.contains("<script")
                || normalized.contains("</script")
                || normalized.contains("javascript:")
                || normalized.contains("onerror=")
                || normalized.contains("onload=")
                || normalized.contains("union select")
                || normalized.contains("drop table")
                || normalized.contains("insert into")
                || normalized.contains("delete from")
                || normalized.contains("update ")
                || normalized.contains("--")
                || normalized.contains("/*")
                || normalized.contains("*/")
                || normalized.contains("xp_")
                || normalized.contains(" or 1=1")
                || normalized.contains("' or '1'='1")
                || normalized.contains("\" or \"1\"=\"1")
                || normalized.contains("<")
                || normalized.contains(">");
    }
}