package com.example.todoappbackend.dto;

public class TodoItemRequestDTO {
    private String description;
    private boolean completed;

    // Constructors
    public TodoItemRequestDTO() {
    }

    public TodoItemRequestDTO(String description, boolean completed) {
        this.description = description;
        this.completed = completed;
    }

    // Getters and Setters
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
