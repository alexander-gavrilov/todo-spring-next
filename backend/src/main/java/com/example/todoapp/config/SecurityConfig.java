package com.example.todoapp.config;

import com.example.todoapp.service.oauth2.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/index.html", "/static/**", "/*.png", "/*.ico", "/*.json", "/assets/**").permitAll() // Frontend assets
                .requestMatchers("/error").permitAll()
                .requestMatchers("/oauth2/**", "/login/**").permitAll() // OAuth2 and login related paths
                .requestMatchers("/h2-console/**").permitAll() // Allow H2 console access
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                //.defaultSuccessUrl("http://localhost:5173", true) // Redirect to frontend after login
                .failureUrl("/login?error=true") // Basic failure handling
            )
            .logout(logout -> logout
                .logoutUrl("/api/logout") // Define a custom logout URL if needed
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK)) // Send 200 OK on logout
                .deleteCookies("JSESSIONID") // Delete session cookie
                .invalidateHttpSession(true) // Invalidate session
                .clearAuthentication(true)
                .permitAll()
            )
            .csrf(csrf -> csrf
                // For development with H2 console, disable CSRF or configure it properly.
                // Disabling for H2 console, but for APIs, we might want it.
                // If frontend and backend are on different domains, CSRF with cookies can be tricky.
                // For now, making it lax for /h2-console and APIs.
                // A common approach for SPAs is to use CookieCsrfTokenRepository and have JS read the cookie.
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/h2-console/**", "/api/**") // TEMPORARY: For APIs, if using tokens, CSRF might not be needed. If using sessions, it is.
            )
            // For H2 console to work with Spring Security, frame options need to be disabled or sameOrigin.
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
            .exceptionHandling(e -> e
                // If an unauthenticated user tries to access a protected resource, send 401 instead of redirecting to login page
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource())); // Apply CORS configuration

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173")); // Vite default port
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true); // Important for cookies, authorization headers with HTTPS
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
