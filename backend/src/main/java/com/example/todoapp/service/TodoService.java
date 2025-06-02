package com.example.todoapp.service;

import com.example.todoapp.model.dto.TodoDto;
import com.example.todoapp.model.entity.TodoEntity;
import com.example.todoapp.model.entity.UserEntity;
import com.example.todoapp.repository.TodoRepository;
import com.example.todoapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    @Autowired
    public TodoService(TodoRepository todoRepository, UserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    // Helper method to get current authenticated user (will be refined)
    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof OAuth2User)) {
            // This case should ideally be handled by security config redirecting to login
            throw new IllegalStateException("User not authenticated or authentication principal is not OAuth2User");
        }
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        // Assuming the 'sub' or 'id' attribute from OAuth2 provider is used as externalId
        // and provider is stored in UserEntity. This part will be more robust later.
        String externalId = oauth2User.getName(); // .getName() often returns the subject ID for OAuth2User

        // The provider information would ideally be part of the OAuth2AuthenticationToken or similar
        // For now, this is a placeholder. This logic will be centralized in user processing.
        // We'll need to fetch the provider from the OAuth2AuthenticationToken's registrationId.
        // This is a simplified placeholder.
        String determinedProvider = "unknown"; // Placeholder - this needs to be determined from the OAuth2 token
         if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("google"))) {
            determinedProvider = "google";
        } else if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("facebook"))) {
            determinedProvider = "facebook";
        } else if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("microsoft"))) {
            determinedProvider = "microsoft";
        }

        final String finalProvider = determinedProvider;
        // This is a simplified lookup. A more robust solution would involve getting the provider
        // from the OAuth2AuthenticationToken.getAuthorizedClientRegistrationId()
        // For now, we'll assume a single provider or that externalId is globally unique.
        // This needs to be properly implemented with OAuth2 user details service.
        return userRepository.findByExternalIdAndProvider(externalId, finalProvider)
                .orElseThrow(() -> new IllegalStateException("User not found in database. Authentication principal: " + externalId + ", Provider: " + finalProvider + ". OAuth2User attributes: " + oauth2User.getAttributes()));
    }


    @Transactional(readOnly = true)
    public List<TodoDto> getTodosForCurrentUser() {
        UserEntity currentUser = getCurrentUser();
        return todoRepository.findByUserId(currentUser.getId()).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TodoDto createTodoForCurrentUser(TodoDto todoDto) {
        UserEntity currentUser = getCurrentUser();
        TodoEntity todoEntity = new TodoEntity(todoDto.getTitle(), todoDto.getDescription(), currentUser);
        todoEntity.setCompleted(todoDto.isCompleted());
        todoEntity = todoRepository.save(todoEntity);
        return convertToDto(todoEntity);
    }

    @Transactional
    public Optional<TodoDto> updateTodoForCurrentUser(Long todoId, TodoDto todoDto) {
        UserEntity currentUser = getCurrentUser();
        Optional<TodoEntity> existingTodoOpt = todoRepository.findById(todoId);

        if (existingTodoOpt.isPresent()) {
            TodoEntity existingTodo = existingTodoOpt.get();
            if (!existingTodo.getUser().getId().equals(currentUser.getId())) {
                // User is trying to update a todo that doesn't belong to them
                throw new SecurityException("User not authorized to update this todo.");
            }
            existingTodo.setTitle(todoDto.getTitle());
            existingTodo.setDescription(todoDto.getDescription());
            existingTodo.setCompleted(todoDto.isCompleted());
            existingTodo = todoRepository.save(existingTodo);
            return Optional.of(convertToDto(existingTodo));
        }
        return Optional.empty(); // Todo not found
    }

    @Transactional
    public boolean deleteTodoForCurrentUser(Long todoId) {
        UserEntity currentUser = getCurrentUser();
        Optional<TodoEntity> todoOpt = todoRepository.findById(todoId);

        if (todoOpt.isPresent()) {
            TodoEntity todo = todoOpt.get();
            if (!todo.getUser().getId().equals(currentUser.getId())) {
                // User is trying to delete a todo that doesn't belong to them
                throw new SecurityException("User not authorized to delete this todo.");
            }
            todoRepository.deleteById(todoId);
            return true;
        }
        return false; // Todo not found
    }

    private TodoDto convertToDto(TodoEntity entity) {
        return new TodoDto(entity.getId(), entity.getTitle(), entity.getDescription(), entity.isCompleted());
    }
}
