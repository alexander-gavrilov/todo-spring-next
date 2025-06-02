package com.example.todoapp.controller;

import com.example.todoapp.model.dto.TodoDto;
import com.example.todoapp.service.TodoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@WebMvcTest(TodoController.class)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoService todoService;

    @Autowired
    private ObjectMapper objectMapper;

    private OAuth2User testUser;
    private TodoDto sampleTodoDto1;
    private TodoDto sampleTodoDto2;

    @BeforeEach
    void setUp() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "test-user-sub"); // Standard attribute for user's unique ID
        attributes.put("name", "Test User");
        attributes.put("email", "testuser@example.com");

        testUser = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "sub"); // nameAttributeKey, often "sub", "id", or "login"

        sampleTodoDto1 = new TodoDto(1L, "Test Todo 1", "Description 1", false);
        sampleTodoDto2 = new TodoDto(2L, "Test Todo 2", "Description 2", true);
    }

    // --- Test GET /api/todos (getTodos) ---
    @Test
    void getTodos_WhenServiceReturnsListOfTodos_ShouldReturnOkAndListOfTodos() throws Exception {
        List<TodoDto> todos = Arrays.asList(sampleTodoDto1, sampleTodoDto2);
        when(todoService.getTodosForCurrentUser()).thenReturn(todos);

        mockMvc.perform(get("/api/todos")
                        .with(oauth2Login().oauth2User(testUser))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is(sampleTodoDto1.getTitle())))
                .andExpect(jsonPath("$[1].title", is(sampleTodoDto2.getTitle())));
    }

    @Test
    void getTodos_WhenServiceReturnsEmptyList_ShouldReturnOkAndEmptyList() throws Exception {
        when(todoService.getTodosForCurrentUser()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/todos")
                        .with(oauth2Login().oauth2User(testUser))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getTodos_WhenServiceThrowsIllegalStateException_ShouldReturnUnauthorized() throws Exception {
        when(todoService.getTodosForCurrentUser()).thenThrow(new IllegalStateException("User not authenticated"));

        mockMvc.perform(get("/api/todos")
                        .with(oauth2Login().oauth2User(testUser)) // Still need a principal for the filter chain
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // --- Test POST /api/todos (createTodo) ---
    @Test
    void createTodo_WhenServiceCreatesTodo_ShouldReturnCreatedAndTodo() throws Exception {
        when(todoService.createTodoForCurrentUser(any(TodoDto.class))).thenReturn(sampleTodoDto1);

        mockMvc.perform(post("/api/todos")
                        .with(oauth2Login().oauth2User(testUser))
                        .with(csrf()) // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTodoDto1)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title", is(sampleTodoDto1.getTitle())));
    }

    @Test
    void createTodo_WhenServiceThrowsIllegalStateException_ShouldReturnUnauthorized() throws Exception {
        when(todoService.createTodoForCurrentUser(any(TodoDto.class))).thenThrow(new IllegalStateException("User not authenticated"));

        mockMvc.perform(post("/api/todos")
                        .with(oauth2Login().oauth2User(testUser))
                        .with(csrf()) // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTodoDto1)))
                .andExpect(status().isUnauthorized());
    }


    // --- Test PUT /api/todos/{id} (updateTodo) ---
    @Test
    void updateTodo_WhenServiceUpdatesTodo_ShouldReturnOkAndUpdatedTodo() throws Exception {
        TodoDto updatedDto = new TodoDto(1L, "Updated Title", "Updated Desc", true);
        when(todoService.updateTodoForCurrentUser(anyLong(), any(TodoDto.class))).thenReturn(Optional.of(updatedDto));

        mockMvc.perform(put("/api/todos/{id}", 1L)
                        .with(oauth2Login().oauth2User(testUser))
                        .with(csrf()) // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title", is(updatedDto.getTitle())))
                .andExpect(jsonPath("$.completed", is(true)));
    }

    @Test
    void updateTodo_WhenServiceReturnsEmptyOptional_ShouldReturnNotFound() throws Exception {
        when(todoService.updateTodoForCurrentUser(anyLong(), any(TodoDto.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/todos/{id}", 1L)
                        .with(oauth2Login().oauth2User(testUser))
                        .with(csrf()) // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTodoDto1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTodo_WhenServiceThrowsSecurityException_ShouldReturnForbidden() throws Exception {
        when(todoService.updateTodoForCurrentUser(anyLong(), any(TodoDto.class)))
            .thenThrow(new SecurityException("User not authorized"));

        mockMvc.perform(put("/api/todos/{id}", 1L)
                        .with(oauth2Login().oauth2User(testUser))
                        .with(csrf()) // Add CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTodoDto1)))
                .andExpect(status().isForbidden());
    }

    // --- Test DELETE /api/todos/{id} (deleteTodo) ---
    @Test
    void deleteTodo_WhenServiceDeletesTodo_ShouldReturnNoContent() throws Exception {
        when(todoService.deleteTodoForCurrentUser(anyLong())).thenReturn(true);

        mockMvc.perform(delete("/api/todos/{id}", 1L)
                        .with(oauth2Login().oauth2User(testUser))
                        .with(csrf())) // Add CSRF token
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTodo_WhenServiceFailsToDelete_ShouldReturnNotFound() throws Exception {
        when(todoService.deleteTodoForCurrentUser(anyLong())).thenReturn(false);

        mockMvc.perform(delete("/api/todos/{id}", 1L)
                        .with(oauth2Login().oauth2User(testUser))
                        .with(csrf())) // Add CSRF token
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTodo_WhenServiceThrowsSecurityException_ShouldReturnForbidden() throws Exception {
        when(todoService.deleteTodoForCurrentUser(anyLong()))
            .thenThrow(new SecurityException("User not authorized"));

        mockMvc.perform(delete("/api/todos/{id}", 1L)
                        .with(oauth2Login().oauth2User(testUser))
                        .with(csrf())) // Add CSRF token
                .andExpect(status().isForbidden());
    }
}
