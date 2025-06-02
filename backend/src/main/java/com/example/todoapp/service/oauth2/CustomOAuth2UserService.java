package com.example.todoapp.service.oauth2;

import com.example.todoapp.model.entity.UserEntity;
import com.example.todoapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String externalId = getExternalId(oauth2User, provider);
        String name = getName(attributes, provider);
        String email = getEmail(attributes, provider);

        Optional<UserEntity> userOptional = userRepository.findByExternalIdAndProvider(externalId, provider);
        UserEntity user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Update user details if changed
            user.setName(name);
            if (email != null) { // Only update email if provider gives one
                 user.setEmail(email);
            }
        } else {
            user = new UserEntity(externalId, name, email, provider);
        }
        userRepository.save(user);

        return oauth2User; // Spring Security will handle creating the Authentication object
    }

    private String getExternalId(OAuth2User oauth2User, String provider) {
        // For Google and Microsoft, 'sub' is standard. Facebook uses 'id'.
        if ("facebook".equalsIgnoreCase(provider)) {
            return oauth2User.getAttribute("id");
        }
        return oauth2User.getAttribute("sub"); // Standard OpenID Connect subject identifier
    }

    private String getName(Map<String, Object> attributes, String provider) {
        if ("google".equalsIgnoreCase(provider) || "microsoft".equalsIgnoreCase(provider)) {
            return (String) attributes.get("name");
        } else if ("facebook".equalsIgnoreCase(provider)) {
            return (String) attributes.get("name"); // Facebook also provides 'name'
        }
        return "Unknown User"; // Fallback
    }

    private String getEmail(Map<String, Object> attributes, String provider) {
         // Email might not always be present or verified
        if (attributes.containsKey("email") && attributes.get("email") != null) {
            return (String) attributes.get("email");
        }
        return null; // It's important to handle null emails
    }
}
