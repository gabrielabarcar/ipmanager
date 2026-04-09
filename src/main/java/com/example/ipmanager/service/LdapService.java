package com.example.ipmanager.service;

import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.List;

@Service
public class LdapService {

    private static final List<DomainConfig> DOMAIN_CONFIGS = List.of(
            new DomainConfig("CCSSVMEDDC00", "gmedica.caja.ccss.sa.cr"),
            new DomainConfig("ccssvgfidc01", "gfinan.caja.ccss.sa.cr"),
            new DomainConfig("ccssvgldc03", "gopera.caja.ccss.sa.cr"),
            new DomainConfig("ccssvprdc01", "presidencia.caja.ccss.sa.cr"),
            new DomainConfig("CCSSVMEDDC01", "gmedica.caja.ccss.sa.cr"),

            // agregado para probar tu caso directo
            new DomainConfig("ccssvgldc03", "ccss.sa.cr")
    );

    public boolean authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        if (password == null || password.trim().isEmpty()) {
            return false;
        }

        String normalizedUsername = normalizeUsername(username);

        System.out.println("=== INICIO PRUEBA LDAP ===");
        System.out.println("Usuario original: " + username);
        System.out.println("Usuario normalizado: " + normalizedUsername);

        for (DomainConfig config : DOMAIN_CONFIGS) {
            if (authenticateAgainstDomain(normalizedUsername, password, config)) {
                System.out.println("LDAP OK con servidor: " + config.server + " y dominio: " + config.domain);
                return true;
            }
        }

        System.out.println("LDAP FALLÓ en todos los dominios configurados.");
        return false;
    }

    private String normalizeUsername(String username) {
        String value = username.trim();
        int atIndex = value.indexOf("@");
        if (atIndex > 0) {
            return value.substring(0, atIndex);
        }
        return value;
    }

    private boolean authenticateAgainstDomain(String username, String password, DomainConfig config) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + config.server + ":389");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, username + "@" + config.domain);
        env.put(Context.SECURITY_CREDENTIALS, password);

        String principal = username + "@" + config.domain;
        System.out.println("Probando LDAP con: " + principal + " en ldap://" + config.server + ":389");

        try {
            new InitialDirContext(env);
            return true;
        } catch (NamingException e) {
            System.out.println("Falló LDAP con: " + principal + " | Motivo: " + e.getMessage());
            return false;
        }
    }

    private static class DomainConfig {
        private final String server;
        private final String domain;

        public DomainConfig(String server, String domain) {
            this.server = server;
            this.domain = domain;
        }
    }
}