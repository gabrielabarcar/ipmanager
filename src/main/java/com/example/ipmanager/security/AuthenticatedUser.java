package com.example.ipmanager.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String nombre;
    private final String apellido1;
    private final String apellido2;
    private final String correo;
    private final String role;

    public AuthenticatedUser(Long id,
                             String username,
                             String password,
                             Boolean enabled,
                             Collection<? extends GrantedAuthority> authorities,
                             String nombre,
                             String apellido1,
                             String apellido2,
                             String correo,
                             String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities;
        this.nombre = nombre;
        this.apellido1 = apellido1;
        this.apellido2 = apellido2;
        this.correo = correo;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido1() {
        return apellido1;
    }

    public String getApellido2() {
        return apellido2;
    }

    public String getCorreo() {
        return correo;
    }

    public String getRole() {
        return role;
    }

    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder();

        if (nombre != null && !nombre.isBlank()) {
            sb.append(nombre.trim());
        }
        if (apellido1 != null && !apellido1.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(apellido1.trim());
        }
        if (apellido2 != null && !apellido2.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(apellido2.trim());
        }

        return sb.toString().trim();
    }

    public String getNombreMostrado() {
        String nombreCompleto = getNombreCompleto();
        return !nombreCompleto.isBlank() ? nombreCompleto : username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password != null ? password : "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}