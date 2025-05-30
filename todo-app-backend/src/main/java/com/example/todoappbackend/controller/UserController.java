package com.example.todoappbackend.controller;

import com.example.todoappbackend.model.User;
import com.example.todoappbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // The CustomOAuth2UserService is configured to use "email" as the name attribute key for the Principal.
        // So, principal.getName() should return the email.
        String email = principal.getName(); 
        
        // Alternative: Extract from attributes if principal.getName() is not the email
        // String email = principal.getAttribute("email");
        // if (email == null) {
        //     // Handle cases where email might not be directly under "email" key
        //     // This logic would mirror attribute extraction in CustomOAuth2UserService
        //     if (principal.getAttribute("preferred_username") != null && ((String)principal.getAttribute("preferred_username")).contains("@")) {
        //         email = principal.getAttribute("preferred_username");
        //     } else {
        //          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Email attribute not found in OAuth2 principal");
        //     }
        // }
        
        // Assuming CustomOAuth2UserService ensures user exists or creates one,
        // and that principal.getName() is the email.
        // UserService would then have a method like findUserByEmail or similar.
        // For now, assuming processOAuthPostLogin in UserService also ensures user is in DB,
        // and we can retrieve it via email.
        // Let's define a new method in UserService to get user by email,
        // or adapt processOAuthPostLogin if it can also serve as a getter post-auth.

        // For simplicity, let's assume UserService gets a findByEmail method or similar
        // For now, I'll stick to UserRepository for findByEmail as UserService's processOAuthPostLogin is for creation/update.
        // This highlights a point for potential refactoring in UserService to expose a getter.
        // Sticking to the plan of using UserService, I should add a method there.
        // However, the current UserService.processOAuthPostLogin is for writing.
        // Let's assume for now that if a user is authenticated, CustomOAuth2UserService has ALREADY
        // processed them via userService.processOAuthPostLogin, so the user IS in the DB.
        // We can then use UserRepository to fetch, or add a specific getter to UserService.
        // Given the instructions "Use userService to find the user by email", I will assume
        // that UserService should have such a method. I will add it if it's not there.
        // For now, I will call a hypothetical findByEmail on userService.
        // This will fail compilation if such method doesn't exist, prompting its addition to UserService.

        // Let's use the provider and providerId as that's what processOAuthPostLogin uses for lookup.
        // This requires provider and providerId to be reliably available in the principal.
        // The principal.getName() is often the 'sub' or 'id' (providerId).
        // The provider string is not directly in OAuth2User principal by default, it was from ClientRegistration.
        // This makes using userService.processOAuthPostLogin directly for fetching tricky from controller.

        // Fallback: Use the email from principal, which is set as the "name" for the security principal.
        // And then use UserRepository directly, or add a findByEmail to UserService.
        // The prompt "Use userService to find the user by email" implies UserService should have this.

        // Let's assume UserService will be augmented with:
        // public Optional<User> findUserByEmail(String email) { return userRepository.findByEmail(email); }
        // I will make this change to UserService in a subsequent step if current one fails compilation.
        // For now, using userRepository as previously, and will adjust if the overall plan requires UserService here.
        // Given the prompt specified "Inject UserService", I will use it and assume it has/will have findByEmail.

        // Re-evaluating: The task says "Use userService to find the user by email".
        // The most straightforward way is to add this method to UserService.
        // I will proceed with this assumption and add the method to UserService next if it causes a build failure.
        // For now, I'll write the code AS IF `userService.findUserByEmail(email)` exists.

        Optional<User> userOptional = userService.findUserByEmail(email); // This method needs to be added to UserService

        if (userOptional.isPresent()) {
            return ResponseEntity.ok(userOptional.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found in database: " + email);
        }
    }
}
