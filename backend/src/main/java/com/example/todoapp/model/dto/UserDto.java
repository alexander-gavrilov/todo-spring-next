package com.example.todoapp.model.dto;

import java.util.Map;
import java.util.Set;

public class UserDto {
    private String id; // Usually the 'sub' from OAuth2 provider
    private String name;
    private String email;
    private String provider;
    // private Map<String, Object> attributes; // Optionally send all attributes
    // private Set<String> roles; // Optionally send roles/authorities

    public UserDto(String id, String name, String email, String provider) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.provider = provider;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    // public Map<String, Object> getAttributes() { return attributes; }
    // public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}
