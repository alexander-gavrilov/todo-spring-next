package com.example.todoappbackend.security;

import com.example.todoappbackend.model.User;
import com.example.todoappbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    @Autowired
    public CustomOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = null;
        String name = null;
        String email = null;

        // Attempt to extract common attributes
        // Google: sub, name, email
        // Facebook: id, name, email
        // GitHub: id, login (name), email (can be null)
        // Defaulting to standard names, may need provider-specific handling
        
        if (attributes.containsKey("sub")) { // Google, Microsoft (sometimes)
            providerId = String.valueOf(attributes.get("sub"));
        } else if (attributes.containsKey("id")) { // Facebook, GitHub
            providerId = String.valueOf(attributes.get("id"));
        } else if (attributes.containsKey("oid")) { // Microsoft (alternative)
             providerId = String.valueOf(attributes.get("oid"));
        }


        if (attributes.containsKey("name")) {
            name = (String) attributes.get("name");
        } else if (attributes.containsKey("login")) { // GitHub uses 'login' as display name
            name = (String) attributes.get("login");
        }


        if (attributes.containsKey("email")) {
            email = (String) attributes.get("email");
        } else if (attributes.containsKey("preferred_username") && ((String)attributes.get("preferred_username")).contains("@")) { // Microsoft
            email = (String) attributes.get("preferred_username");
        }
        
        // Fallback if name is not directly available but given_name and family_name are (common in OIDC)
        if (name == null && attributes.containsKey("given_name") && attributes.containsKey("family_name")) {
            name = attributes.get("given_name") + " " + attributes.get("family_name");
        }
         // Fallback for providerId if it's somehow still null (should be very rare)
        if (providerId == null) {
            providerId = oauth2User.getName(); // getName() often returns the subject (ID)
        }
        // Fallback for email if not found, though it's usually crucial
        if (email == null && providerId != null) {
            // Create a placeholder email if absolutely necessary, or throw error
            // This is not ideal and depends on application requirements for email
            email = providerId + "@" + provider + ".example.com"; // Placeholder, not recommended for production
        }
         // Fallback for name
        if (name == null && providerId != null) {
            name = providerId; // Use providerId as name if nothing else is available
        }


        if (providerId == null || email == null) {
            throw new OAuth2AuthenticationException("Essential user attributes (providerId, email) not found from OAuth2 provider.");
        }


        User internalUser = userService.processOAuthPostLogin(provider, providerId, name, email);

        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        // Create a Spring Security User principal
        // Using email as username for Spring Security context, ensure it's unique
        org.springframework.security.core.userdetails.User securityUser =
                new org.springframework.security.core.userdetails.User(internalUser.getEmail(), "", authorities);
        
        // It's often better to return a custom OAuth2User implementation that wraps your internal User entity
        // and also provides the original OAuth2User attributes and authorities.
        // For now, returning a standard OAuth2User with merged details.
        // DefaultOAuth2User(Set<GrantedAuthority> authorities, Map<String, Object> attributes, String nameAttributeKey)
        // The 'nameAttributeKey' is the key in the attributes map that represents the user's name/ID.
        // Using 'email' as the name attribute key for the principal's name in Spring Security context.
        
        // Re-wrap with potentially updated attributes from our internalUser if needed,
        // or just use the original oauth2User attributes and authorities but ensure principal name is consistent.
        // For simplicity, we will create a new DefaultOAuth2User using email as the 'name' attribute for the Principal.
        // The attributes map can be the original one, or a modified one.
        // The authorities are what we've decided for our app.

        return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                authorities,
                attributes, // Using original attributes from provider
                "email" // The key in 'attributes' map to use for Principal.getName()
        );
    }
}
