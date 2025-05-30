package com.example.todoappbackend.controller;

import com.example.todoappbackend.dto.TodoItemRequestDTO;
import com.example.todoappbackend.exception.UserNotFoundException;
import com.example.todoappbackend.model.TodoItem;
import com.example.todoappbackend.model.User;
import com.example.todoappbackend.service.TodoService;
import com.example.todoappbackend.service.UserService; // Or UserRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;
    private final UserService userService; // Using UserService as per general good practice

    @Autowired
    public TodoController(TodoService todoService, UserService userService) {
        this.todoService = todoService;
        this.userService = userService;
    }

    private User getAuthenticatedUser(OAuth2User principal) {
        if (principal == null) {
            throw new UserNotFoundException("User not authenticated. Cannot perform operation.");
        }
        String email = principal.getName(); // Assuming email is the name identifier in principal
        // This relies on CustomOAuth2UserService setting the email as the principal's name,
        // and UserService having a method to find by email.
        return userService.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found in database with email: " + email));
    }

    @GetMapping
    public List<TodoItem> getTodos(@AuthenticationPrincipal OAuth2User principal) {
        User appUser = getAuthenticatedUser(principal);
        return todoService.getTodosByUserId(appUser.getId());
    }

    @PostMapping
    public ResponseEntity<TodoItem> createTodo(@RequestBody TodoItemRequestDTO todoDto,
                                               @AuthenticationPrincipal OAuth2User principal) {
        User appUser = getAuthenticatedUser(principal);
        TodoItem createdTodo = todoService.createTodo(todoDto.getDescription(), appUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTodo);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TodoItem> getTodoById(@PathVariable Long id,
                                                @AuthenticationPrincipal OAuth2User principal) {
        User appUser = getAuthenticatedUser(principal);
        // Using .orElse(null) to let GlobalExceptionHandler handle it if service throws exception
        return todoService.getTodoByIdAndUserId(id, appUser.getId()) 
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build()); // Should be caught by GlobalExceptionHandler if service throws Not Found or Access Denied
    }


    @PutMapping("/{id}")
    public ResponseEntity<TodoItem> updateTodo(@PathVariable Long id,
                                               @RequestBody TodoItemRequestDTO todoDto,
                                               @AuthenticationPrincipal OAuth2User principal) {
        User appUser = getAuthenticatedUser(principal);
        // Exceptions from service (TodoItemNotFound, AccessDenied) will be handled by GlobalExceptionHandler
        TodoItem updatedTodo = todoService.updateTodo(id, todoDto.getDescription(), todoDto.isCompleted(), appUser.getId());
        return ResponseEntity.ok(updatedTodo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(@PathVariable Long id,
                                           @AuthenticationPrincipal OAuth2User principal) {
        User appUser = getAuthenticatedUser(principal);
        // Exceptions from service (TodoItemNotFound, AccessDenied) will be handled by GlobalExceptionHandler
        todoService.deleteTodo(id, appUser.getId());
        return ResponseEntity.noContent().build();
    }
}
