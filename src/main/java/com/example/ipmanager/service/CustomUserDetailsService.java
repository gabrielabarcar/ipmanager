package com.example.ipmanager.service;

import com.example.ipmanager.model.AppUser;
import com.example.ipmanager.repository.AppUserRepository;
import com.example.ipmanager.security.AuthenticatedUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public CustomUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado."));

        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())),
                user.getNombre(),
                user.getApellido1(),
                user.getApellido2(),
                user.getCorreo(),
                user.getRole()
        );
    }
}