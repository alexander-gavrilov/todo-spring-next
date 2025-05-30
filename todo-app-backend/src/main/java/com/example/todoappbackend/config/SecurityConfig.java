package com.example.todoappbackend.config;

import com.example.todoappbackend.security.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/login/**", "/oauth2/**", "/error").permitAll()
                .requestMatchers("/api/public/**").permitAll() // Example for public endpoints
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .defaultSuccessUrl("/api/user/me", true) // A simple endpoint to verify login
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/").permitAll()
                // Optionally, configure specific logout endpoint, invalidate session, clear cookies
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID") // Adjust cookie name if different
                .logoutUrl("/logout") // Default is /logout
            )
            // CSRF Configuration:
            // Disable CSRF if using stateless REST APIs with tokens.
            // If using session cookies (default with oauth2Login), CSRF protection is generally recommended.
            // For now, disabling to simplify. Revisit if using session-based auth primarily.
            .csrf(csrf -> csrf.disable())
            // HTTP Basic authentication - can be removed if not needed, or kept as a fallback.
            // For an OAuth2-focused app, it might be cleaner to remove if not used.
            .httpBasic(Customizer.withDefaults()); // or .httpBasic(httpBasic -> httpBasic.disable())

        return http.build();
    }
}
