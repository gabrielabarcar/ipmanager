package com.example.ipmanager.security;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
public class SecurityConfig {

    private final HybridAuthenticationProvider hybridAuthenticationProvider;

    public SecurityConfig(HybridAuthenticationProvider hybridAuthenticationProvider) {
        this.hybridAuthenticationProvider = hybridAuthenticationProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        AuthenticationManagerBuilder authBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.authenticationProvider(hybridAuthenticationProvider);

        http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()

                    // REDES
                    .requestMatchers("/").hasAuthority("REDES_VIEW")
                    .requestMatchers("/save", "/delete/**", "/export/**").hasAuthority("REDES_ADMIN")

                    // USUARIOS
                    .requestMatchers("/users").hasAuthority("USUARIOS_VIEW")
                    .requestMatchers("/users/save", "/users/delete/**").hasAuthority("USUARIOS_ADMIN")

                    // BITÁCORA
                    .requestMatchers("/logs/**").hasAuthority("BITACORA_VIEW")

                    // SESIÓN
                    .requestMatchers("/session/**").authenticated()

                    .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureHandler(authenticationFailureHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?sessionExpired")
                        .sessionRegistry(sessionRegistry)
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            String username = request.getParameter("username");
            String target = "/login?error";

            if (exception instanceof UsernameNotFoundException
                    || exception instanceof BadCredentialsException
                    || exception instanceof DisabledException) {
                target = "/login?contactAdmin";
            }

            if (username != null && !username.isBlank()) {
                target += "&lastUsername=" + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);
            }

            response.sendRedirect(target);
        };
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionListener httpSessionListener() {
        return new HttpSessionListener() {
            @Override
            public void sessionDestroyed(HttpSessionEvent se) {
                HttpSessionListener.super.sessionDestroyed(se);
            }
        };
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}