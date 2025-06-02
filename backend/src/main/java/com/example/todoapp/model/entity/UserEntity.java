package com.example.todoapp.model.entity;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String externalId; // To store Google/Facebook/Microsoft ID

    @Column(nullable = false)
    private String name;

    @Column(unique = true) // Email might not always be provided or unique depending on provider policies
    private String email;

    @Column(nullable = false)
    private String provider; // e.g., "google", "facebook", "microsoft"

    // One user can have many todos
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<TodoEntity> todos;

    // Constructors
    public UserEntity() {
    }

    public UserEntity(String externalId, String name, String email, String provider) {
        this.externalId = externalId;
        this.name = name;
        this.email = email;
        this.provider = provider;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Set<TodoEntity> getTodos() {
        return todos;
    }

    public void setTodos(Set<TodoEntity> todos) {
        this.todos = todos;
    }
}
