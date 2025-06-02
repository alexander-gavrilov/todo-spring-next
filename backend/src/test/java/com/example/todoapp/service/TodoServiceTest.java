package com.example.todoapp.service;

import com.example.todoapp.model.dto.TodoDto;
import com.example.todoapp.model.entity.TodoEntity;
import com.example.todoapp.model.entity.UserEntity;
import com.example.todoapp.repository.TodoRepository;
import com.example.todoapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Collection; // Added for GrantedAuthority collection
import org.springframework.security.core.GrantedAuthority; // Added for GrantedAuthority

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TodoService todoService;

    private UserEntity testUser;
    private OAuth2User mockPrincipal;
    private TodoEntity testTodo;
    private TodoDto testTodoDto;

    @BeforeEach
    void setUp() {
        // Setup mock OAuth2User principal
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "testExternalId"); // Corresponds to UserEntity.externalId
        attributes.put("name", "Test User");
        attributes.put("email", "testuser@example.com");
        // Provider will be determined by how Authentication is mocked if needed, or assumed by service
        // For TodoService.getCurrentUser(), the provider is hardcoded to "google" in one path,
        // so we should ensure our mock user matches that if we want that path taken,
        // or mock getAuthorizedClientRegistrationId if a more dynamic approach is taken by service.
        // Let's assume "google" for simplicity based on one of the service's internal paths.
        mockPrincipal = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "sub"); // nameAttributeKey

        testUser = new UserEntity("testExternalId", "Test User", "testuser@example.com", "google");
        testUser.setId(1L);


        testTodo = new TodoEntity();
        testTodo.setId(1L);
        testTodo.setTitle("Test Todo");
        testTodo.setDescription("Test Description");
        testTodo.setCompleted(false);
        testTodo.setUser(testUser);

        testTodoDto = new TodoDto(null, "Test Todo DTO", "Test Description DTO", false);
        // ID is null for creation, will be set by service/repo
    }

    private void mockSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockPrincipal);
        when(authentication.isAuthenticated()).thenReturn(true);
        // This authority check is a simplification from TodoService.getCurrentUser()
        // It might need to be more specific if the service relies on specific authority strings
        when(authentication.getAuthorities()).thenAnswer(invocation -> {
            List<GrantedAuthority> authorities = new java.util.ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER_google"));
            return authorities;
        });


        SecurityContextHolder.setContext(securityContext);

        // Crucial mock for TodoService.getCurrentUser() helper
        when(userRepository.findByExternalIdAndProvider("testExternalId", "google"))
                .thenReturn(Optional.of(testUser));
    }
     private void mockSecurityContextUserNotFound() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockPrincipal); // Principal exists
        when(authentication.isAuthenticated()).thenReturn(true);
        SecurityContextHolder.setContext(securityContext);

        // User not found in DB
        when(userRepository.findByExternalIdAndProvider(anyString(), anyString()))
                .thenReturn(Optional.empty());
    }


    // Tests for getTodosForCurrentUser()
    @Test
    void getTodosForCurrentUser_UserHasTodos_ReturnsListOfTodos() {
        mockSecurityContext();
        when(todoRepository.findByUserId(testUser.getId())).thenReturn(List.of(testTodo));

        List<TodoDto> todos = todoService.getTodosForCurrentUser();

        assertNotNull(todos);
        assertEquals(1, todos.size());
        assertEquals("Test Todo", todos.get(0).getTitle());
        verify(userRepository).findByExternalIdAndProvider("testExternalId", "google");
        verify(todoRepository).findByUserId(testUser.getId());
    }

    @Test
    void getTodosForCurrentUser_UserHasNoTodos_ReturnsEmptyList() {
        mockSecurityContext();
        when(todoRepository.findByUserId(testUser.getId())).thenReturn(Collections.emptyList());

        List<TodoDto> todos = todoService.getTodosForCurrentUser();

        assertNotNull(todos);
        assertTrue(todos.isEmpty());
        verify(userRepository).findByExternalIdAndProvider("testExternalId", "google");
        verify(todoRepository).findByUserId(testUser.getId());
    }


    // Tests for createTodoForCurrentUser(TodoDto todoDto)
    @Test
    void createTodoForCurrentUser_ValidTodo_ReturnsCreatedTodo() {
        mockSecurityContext();
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(invocation -> {
            TodoEntity savedTodo = invocation.getArgument(0);
            savedTodo.setId(1L); // Simulate saving and assigning an ID
            return savedTodo;
        });

        TodoDto createdTodo = todoService.createTodoForCurrentUser(testTodoDto);

        assertNotNull(createdTodo);
        assertEquals(testTodoDto.getTitle(), createdTodo.getTitle());
        assertEquals(1L, createdTodo.getId());
        verify(userRepository).findByExternalIdAndProvider("testExternalId", "google");
        verify(todoRepository).save(any(TodoEntity.class));
    }

    @Test
    void createTodoForCurrentUser_UserNotInDb_ThrowsIllegalStateException() {
        mockSecurityContextUserNotFound(); // Simulate principal exists, but user not in DB

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            todoService.createTodoForCurrentUser(testTodoDto);
        });
        assertTrue(exception.getMessage().contains("User not found in database"));
        verify(userRepository).findByExternalIdAndProvider(anyString(), anyString());
        verify(todoRepository, never()).save(any(TodoEntity.class));
    }


    // Tests for updateTodoForCurrentUser(Long id, TodoDto todoDto)
    @Test
    void updateTodoForCurrentUser_TodoExistsAndBelongsToUser_ReturnsUpdatedTodo() {
        mockSecurityContext();
        when(todoRepository.findById(1L)).thenReturn(Optional.of(testTodo)); // testTodo belongs to testUser
        when(todoRepository.save(any(TodoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TodoDto updatedDto = new TodoDto(1L, "Updated Title", "Updated Description", true);

        Optional<TodoDto> result = todoService.updateTodoForCurrentUser(1L, updatedDto);

        assertTrue(result.isPresent());
        assertEquals("Updated Title", result.get().getTitle());
        assertTrue(result.get().isCompleted());
        verify(userRepository).findByExternalIdAndProvider("testExternalId", "google");
        verify(todoRepository).findById(1L);
        verify(todoRepository).save(any(TodoEntity.class));
    }

    @Test
    void updateTodoForCurrentUser_TodoDoesNotExist_ReturnsEmpty() {
        mockSecurityContext();
        when(todoRepository.findById(99L)).thenReturn(Optional.empty());

        TodoDto updatedDto = new TodoDto(99L, "Non Existent", "", false);
        Optional<TodoDto> result = todoService.updateTodoForCurrentUser(99L, updatedDto);

        assertFalse(result.isPresent());
        verify(userRepository).findByExternalIdAndProvider("testExternalId", "google");
        verify(todoRepository).findById(99L);
        verify(todoRepository, never()).save(any(TodoEntity.class));
    }

    @Test
    void updateTodoForCurrentUser_TodoNotOwnedByUser_ThrowsSecurityException() {
        mockSecurityContext();
        UserEntity otherUser = new UserEntity("otherExternalId", "Other User", "other@e.com", "google");
        otherUser.setId(2L);
        TodoEntity otherUsersTodo = new TodoEntity("Other's Todo", "", otherUser);
        otherUsersTodo.setId(2L);

        when(todoRepository.findById(2L)).thenReturn(Optional.of(otherUsersTodo));
        // testUser (ID 1L) is authenticated, otherUsersTodo.getUser().getId() is 2L

        TodoDto updatedDto = new TodoDto(2L, "Attempted Update", "", false);

        Exception exception = assertThrows(SecurityException.class, () -> {
            todoService.updateTodoForCurrentUser(2L, updatedDto);
        });
        assertEquals("User not authorized to update this todo.", exception.getMessage());

        verify(userRepository).findByExternalIdAndProvider("testExternalId", "google");
        verify(todoRepository).findById(2L);
        verify(todoRepository, never()).save(any(TodoEntity.class));
    }


    // Tests for deleteTodoForCurrentUser(Long id)
    @Test
    void deleteTodoForCurrentUser_TodoExistsAndBelongsToUser_DeletesTodoReturnsTrue() {
        mockSecurityContext();
        when(todoRepository.findById(1L)).thenReturn(Optional.of(testTodo)); // testTodo belongs to testUser
        doNothing().when(todoRepository).deleteById(1L);

        boolean deleted = todoService.deleteTodoForCurrentUser(1L);

        assertTrue(deleted);
        verify(userRepository).findByExternalIdAndProvider("testExternalId", "google");
        verify(todoRepository).findById(1L);
        verify(todoRepository).deleteById(1L);
    }

    @Test
    void deleteTodoForCurrentUser_TodoDoesNotExist_ReturnsFalse() {
        mockSecurityContext();
        when(todoRepository.findById(99L)).thenReturn(Optional.empty());

        boolean deleted = todoService.deleteTodoForCurrentUser(99L);

        assertFalse(deleted);
        verify(userRepository).findByExternalIdAndProvider("testExternalId", "google");
        verify(todoRepository).findById(99L);
        verify(todoRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteTodoForCurrentUser_TodoNotOwnedByUser_ThrowsSecurityException() {
        mockSecurityContext();
        UserEntity otherUser = new UserEntity("otherExtId", "Other User", "other@e.com", "google");
        otherUser.setId(2L);
        TodoEntity otherUsersTodo = new TodoEntity("Other's Todo", "", otherUser);
        otherUsersTodo.setId(2L);

        when(todoRepository.findById(2L)).thenReturn(Optional.of(otherUsersTodo));

        Exception exception = assertThrows(SecurityException.class, () -> {
            todoService.deleteTodoForCurrentUser(2L);
        });
        assertEquals("User not authorized to delete this todo.", exception.getMessage());

        verify(userRepository).findByExternalIdAndProvider("testExternalId", "google");
        verify(todoRepository).findById(2L);
        verify(todoRepository, never()).deleteById(anyLong());
    }
}
