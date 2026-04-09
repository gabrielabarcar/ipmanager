package com.example.ipmanager.security;

import com.example.ipmanager.model.AccessLevel;
import com.example.ipmanager.model.AppUser;
import com.example.ipmanager.model.UserModuleAccess;
import com.example.ipmanager.repository.AppUserRepository;
import com.example.ipmanager.service.LdapService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HybridAuthenticationProvider implements AuthenticationProvider {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final LdapService ldapService;

    public HybridAuthenticationProvider(AppUserRepository appUserRepository,
                                        PasswordEncoder passwordEncoder,
                                        LdapService ldapService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.ldapService = ldapService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials() != null
                ? authentication.getCredentials().toString()
                : "";

        AppUser user = appUserRepository.findWithModuleAccessesByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado."));

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new DisabledException("Usuario deshabilitado.");
        }

        boolean localOk = user.getPassword() != null
                && !user.getPassword().isBlank()
                && passwordEncoder.matches(password, user.getPassword());

        if (!localOk) {
            boolean ldapOk = ldapService.authenticate(username, password);
            if (!ldapOk) {
                throw new BadCredentialsException("Credenciales inválidas.");
            }
        }

        return buildAuthentication(user, password);
    }

    private Authentication buildAuthentication(AppUser user, String password) {
        List<SimpleGrantedAuthority> authorities = buildAuthorities(user);

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                authorities,
                user.getNombre(),
                user.getApellido1(),
                user.getApellido2(),
                user.getCorreo(),
                user.getRole()
        );

        return new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                password,
                authenticatedUser.getAuthorities()
        );
    }

    private List<SimpleGrantedAuthority> buildAuthorities(AppUser user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        if (user.getModuleAccesses() != null && !user.getModuleAccesses().isEmpty()) {
            for (UserModuleAccess access : user.getModuleAccesses()) {
                if (access.getModule() == null || access.getModule().getCode() == null || access.getAccessLevel() == null) {
                    continue;
                }

                String moduleCode = access.getModule().getCode().trim().toUpperCase();
                AccessLevel level = access.getAccessLevel();

                if (level == AccessLevel.VIEWER) {
                    authorities.add(new SimpleGrantedAuthority(moduleCode + "_VIEW"));
                } else if (level == AccessLevel.ADMIN) {
                    authorities.add(new SimpleGrantedAuthority(moduleCode + "_VIEW"));
                    authorities.add(new SimpleGrantedAuthority(moduleCode + "_ADMIN"));
                }
            }
        }

        // Compatibilidad temporal con el role actual
        if (authorities.isEmpty() && user.getRole() != null) {
            String role = user.getRole().trim().toUpperCase();

            if ("ADMIN".equals(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("REDES_VIEW"));
                authorities.add(new SimpleGrantedAuthority("REDES_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("USUARIOS_VIEW"));
                authorities.add(new SimpleGrantedAuthority("USUARIOS_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("BITACORA_VIEW"));
                authorities.add(new SimpleGrantedAuthority("BITACORA_ADMIN"));
            } else if ("VIEWER".equals(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_VIEWER"));
                authorities.add(new SimpleGrantedAuthority("REDES_VIEW"));
            }
        }

        return authorities;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}