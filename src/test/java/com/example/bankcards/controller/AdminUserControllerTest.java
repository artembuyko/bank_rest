package com.example.bankcards.controller;

import com.example.bankcards.Config.TestSecurityConfig;
import com.example.bankcards.dto.Mappers.UserMapper;
import com.example.bankcards.dto.Requests.CreateUserRequest;
import com.example.bankcards.dto.Response.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.service.model.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@WithMockUser(roles = "ADMIN")
@Import(TestSecurityConfig.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @Test
    void createUser_ShouldReturnCreatedUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setPassword("Password123!");
        request.setName("Test User");
        request.setRole("USER");

        UserResponse response = new UserResponse();
        response.setId(1L);
        response.setUsername("testuser");

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService).createUser(any(CreateUserRequest.class));
    }

    @Test
    void getAllUsers_ShouldReturnPageOfUsers() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        UserResponse user1 = new UserResponse();
        user1.setId(1L);
        UserResponse user2 = new UserResponse();
        user2.setId(2L);
        Page<UserResponse> page = new PageImpl<>(List.of(user1, user2), pageable, 2);

        when(userService.getAllUser(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[1].id").value(2L));

        verify(userService).getAllUser(any(Pageable.class));
    }

    @Test
    void getUserById_ShouldReturnUser() throws Exception {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        UserResponse response = new UserResponse();
        response.setId(userId);
        response.setUsername("testuser");

        when(userService.getUserById(userId)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        mockMvc.perform(get("/api/admin/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(userService).getUserById(userId);
        verify(userMapper).toResponse(user);
    }

    @Test
    void blockUser_ShouldReturnOk() throws Exception {
        Long userId = 1L;

        mockMvc.perform(put("/api/admin/users/{userId}/block", userId))
                .andExpect(status().isOk());

        verify(userService).blockUser(userId);
    }

    @Test
    void activateUser_ShouldReturnOk() throws Exception {
        Long userId = 1L;

        mockMvc.perform(put("/api/admin/users/{userId}/activate", userId))
                .andExpect(status().isOk());

        verify(userService).activateUser(userId);
    }

    @Test
    void deleteUser_ShouldReturnNoContent() throws Exception {
        Long userId = 1L;

        mockMvc.perform(delete("/api/admin/users/{userId}", userId))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void endpoints_ShouldReturnForbidden_WhenUserNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/users/1/block"))
                .andExpect(status().isForbidden());
    }
}
