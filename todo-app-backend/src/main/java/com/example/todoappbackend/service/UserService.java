package com.example.todoappbackend.service;

import com.example.todoappbackend.model.User;
import com.example.todoappbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User processOAuthPostLogin(String provider, String providerId, String name, String email) {
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, providerId);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            boolean updated = false;
            if (name != null && !name.equals(user.getName())) {
                user.setName(name);
                updated = true;
            }
            if (email != null && !email.equals(user.getEmail())) {
                // Consider if email updates should be allowed or if they indicate a different account
                // For now, updating email if providerId matches.
                // Also, need to ensure the new email isn't already taken by another user.
                Optional<User> userByNewEmail = userRepository.findByEmail(email);
                if (userByNewEmail.isPresent() && !userByNewEmail.get().getId().equals(user.getId())) {
                    // If the new email is already taken by a different user, this could be an issue.
                    // For simplicity, we'll throw an exception or handle as per business logic.
                    // Here, let's assume for now we prevent this update if email conflict.
                    // Or, if providerId is the ultimate source of truth, we might merge accounts or prioritize.
                    // For this example, let's just update if it doesn't conflict or is the same user.
                     throw new IllegalStateException("Email " + email + " is already associated with another account.");
                }
                user.setEmail(email);
                updated = true;
            }
            if (updated) {
                return userRepository.save(user);
            }
            return user;
        } else {
            // Check if user exists by email, if so, this OAuth ID might be new for an existing email.
            // This logic can be complex (e.g., link accounts or throw error).
            // For now, we assume if providerId doesn't match, it's a new user.
            // However, if an account with this email but different provider details exists,
            // it might be better to ask the user to link accounts or log in via the original method.
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                // Handling for existing email but new provider/providerId
                // This could mean the user signed up manually or with another OAuth provider using the same email
                // For now, we'll throw an error to prevent duplicate email entries if providerId is different.
                // A more sophisticated approach might involve account linking.
                throw new IllegalStateException("User with email " + email + " already exists. Try logging in with the original method or link your accounts.");
            }

            User newUser = new User();
            newUser.setProvider(provider);
            newUser.setProviderId(providerId);
            newUser.setName(name);
            newUser.setEmail(email);
            return userRepository.save(newUser);
        }
    }
}
