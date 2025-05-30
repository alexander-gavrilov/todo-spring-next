package com.example.todoappbackend.service;

import com.example.todoappbackend.exception.AccessDeniedException;
import com.example.todoappbackend.exception.TodoItemNotFoundException;
import com.example.todoappbackend.exception.UserNotFoundException;
import com.example.todoappbackend.model.TodoItem;
import com.example.todoappbackend.model.User;
import com.example.todoappbackend.repository.TodoItemRepository;
import com.example.todoappbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TodoService {

    private final TodoItemRepository todoItemRepository;
    private final UserRepository userRepository;

    @Autowired
    public TodoService(TodoItemRepository todoItemRepository, UserRepository userRepository) {
        this.todoItemRepository = todoItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TodoItem createTodo(String description, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        TodoItem todoItem = new TodoItem(description, user);
        return todoItemRepository.save(todoItem);
    }

    public List<TodoItem> getTodosByUserId(Long userId) {
        // Ensure user exists before fetching their todos, or let it return empty if user has no todos.
        // For consistency, it might be good to check if user exists first.
        if (!userRepository.existsById(userId)) {
            // Depending on requirements, either return empty list or throw UserNotFoundException
            // Throwing exception makes it consistent with createTodo
             throw new UserNotFoundException("User not found with id: " + userId);
        }
        return todoItemRepository.findByUserId(userId);
    }

    public Optional<TodoItem> getTodoByIdAndUserId(Long todoId, Long userId) {
        Optional<TodoItem> todoItemOptional = todoItemRepository.findById(todoId);
        if (todoItemOptional.isPresent()) {
            TodoItem todoItem = todoItemOptional.get();
            if (!todoItem.getUser().getId().equals(userId)) {
                // Throw access denied because the item exists but doesn't belong to this user
                throw new AccessDeniedException("User does not have permission to access this todo item.");
            }
            return Optional.of(todoItem);
        }
        return Optional.empty(); // Or throw TodoItemNotFoundException if preferred
    }

    @Transactional
    public TodoItem updateTodo(Long todoId, String description, boolean completed, Long userId) {
        TodoItem todoItem = getTodoByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoItemNotFoundException("TodoItem not found with id: " + todoId + " for user: " + userId));
        
        if (description != null) {
            todoItem.setDescription(description);
        }
        todoItem.setCompleted(completed);
        return todoItemRepository.save(todoItem);
    }

    @Transactional
    public void deleteTodo(Long todoId, Long userId) {
        TodoItem todoItem = getTodoByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new TodoItemNotFoundException("TodoItem not found with id: " + todoId + " for user: " + userId));
        todoItemRepository.delete(todoItem);
    }
}
