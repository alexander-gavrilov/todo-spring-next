package com.example.todoapp.controller;

import com.example.todoapp.model.dto.UserDto;
import com.example.todoapp.model.entity.UserEntity;
import com.example.todoapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    private OAuth2User principal;
    private OAuth2AuthenticationToken authenticationToken;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "test-user-sub"); // For Google/Microsoft
        attributes.put("id", "test-user-id");   // For Facebook
        attributes.put("name", "Test User");
        attributes.put("email", "testuser@example.com");

        principal = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "sub"); // nameAttributeKey, "sub" for google, "id" for facebook, "login" for github

        userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setExternalId("test-user-sub");
        userEntity.setProvider("google");
        userEntity.setName("Test User DB");
        userEntity.setEmail("testuserdb@example.com");
    }

    private OAuth2AuthenticationToken createToken(OAuth2User principal, String provider) {
        return new OAuth2AuthenticationToken(
                principal,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                provider);
    }

    @Test
    void getCurrentUser_UserExistsInRepository_GoogleProvider_ReturnsUserDtoFromDb() throws Exception {
        authenticationToken = createToken(principal, "google");
        // Adjust principal's nameAttributeKey if necessary for "google" if it's not "sub"
        // DefaultOAuth2User uses the second argument of constructor as the nameAttributeKey
        // For Google, "sub" is typically the unique ID. "name" and "email" are other attributes.

        when(userRepository.findByExternalIdAndProvider("test-user-sub", "google"))
                .thenReturn(Optional.of(userEntity));

        mockMvc.perform(get("/api/user/me")
                        .with(authentication(authenticationToken))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userEntity.getExternalId())) // UserController uses externalId as "id" in DTO
                .andExpect(jsonPath("$.name").value(userEntity.getName()))
                .andExpect(jsonPath("$.email").value(userEntity.getEmail()))
                .andExpect(jsonPath("$.provider").value(userEntity.getProvider()));
    }

    @Test
    void getCurrentUser_UserExistsInRepository_FacebookProvider_ReturnsUserDtoFromDb() throws Exception {
        // principal already has "id" attribute for facebook
         Map<String, Object> fbAttributes = new HashMap<>(principal.getAttributes());
        // For facebook, the nameAttributeKey for DefaultOAuth2User should be "id"
        OAuth2User fbPrincipal = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                fbAttributes,
                "id");
        authenticationToken = createToken(fbPrincipal, "facebook");

        UserEntity fbUserEntity = new UserEntity();
        fbUserEntity.setId(2L);
        fbUserEntity.setExternalId("test-user-id"); // Matches principal's "id"
        fbUserEntity.setProvider("facebook");
        fbUserEntity.setName("FB User DB");
        fbUserEntity.setEmail("fbuserdb@example.com");

        when(userRepository.findByExternalIdAndProvider("test-user-id", "facebook"))
                .thenReturn(Optional.of(fbUserEntity));

        mockMvc.perform(get("/api/user/me")
                        .with(authentication(authenticationToken))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(fbUserEntity.getExternalId()))
                .andExpect(jsonPath("$.name").value(fbUserEntity.getName()))
                .andExpect(jsonPath("$.email").value(fbUserEntity.getEmail()))
                .andExpect(jsonPath("$.provider").value(fbUserEntity.getProvider()));
    }


    @Test
    void getCurrentUser_UserNotInRepository_ReturnsUserDtoFromPrincipal() throws Exception {
        authenticationToken = createToken(principal, "google");
        when(userRepository.findByExternalIdAndProvider("test-user-sub", "google"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/user/me")
                        .with(authentication(authenticationToken))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Fallback creates DTO from principal
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("test-user-sub")) // externalId from principal
                .andExpect(jsonPath("$.name").value("Test User"))      // name from principal
                .andExpect(jsonPath("$.email").value("testuser@example.com")) // email from principal
                .andExpect(jsonPath("$.provider").value("google"));     // provider from token
    }

    @Test
    void getCurrentUser_NoPrincipal_RedirectsToLogin() throws Exception {
        // No .with(authentication(...)) means no principal, should redirect to login for a protected endpoint
        mockMvc.perform(get("/api/user/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isFound()) // Expects 302 Redirect
                .andExpect(redirectedUrlPattern("**/login")); // Checks if redirected to a login page
    }
}
