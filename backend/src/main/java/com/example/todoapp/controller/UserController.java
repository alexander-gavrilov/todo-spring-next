package com.example.todoapp.controller;

import com.example.todoapp.model.dto.UserDto;
import com.example.todoapp.model.entity.UserEntity; // Import UserEntity
import com.example.todoapp.repository.UserRepository; // Import UserRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository; // Autowire UserRepository

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal OAuth2User principal, OAuth2AuthenticationToken authentication) {
        if (principal == null) {
            return ResponseEntity.notFound().build();
        }

        String provider = authentication.getAuthorizedClientRegistrationId();
        String externalId;
        String name = principal.getAttribute("name");
        String email = principal.getAttribute("email");

        // Determine externalId based on provider
        if ("facebook".equalsIgnoreCase(provider)) {
            externalId = principal.getAttribute("id");
        } else { // google, microsoft use "sub"
            externalId = principal.getAttribute("sub");
        }

        if (name == null && "github".equalsIgnoreCase(provider)) { // Example if adding GitHub later
             name = principal.getAttribute("login");
        }


        // Optionally, fetch more details from your UserEntity if needed
        // For example, to get your internal user ID or roles stored in your DB
        // This also ensures the user exists in your DB as per CustomOAuth2UserService logic
        Optional<UserEntity> userEntityOpt = userRepository.findByExternalIdAndProvider(externalId, provider);
        if (userEntityOpt.isPresent()) {
            UserEntity dbUser = userEntityOpt.get();
            // You can override name/email with what's in your DB if desired, or add more info
            // For now, mostly using info directly from principal, ensuring consistency with what CustomOAuth2UserService might store
            return ResponseEntity.ok(new UserDto(dbUser.getExternalId(), dbUser.getName(), dbUser.getEmail(), dbUser.getProvider()));
        } else {
             // This case should ideally not happen if CustomOAuth2UserService ran correctly.
             // But as a fallback, can create a DTO from principal directly.
             System.err.println("User not found in DB for /me endpoint, but was authenticated. ExternalID: " + externalId + ", Provider: " + provider);
             return ResponseEntity.ok(new UserDto(externalId, name, email, provider));
        }
    }
}
