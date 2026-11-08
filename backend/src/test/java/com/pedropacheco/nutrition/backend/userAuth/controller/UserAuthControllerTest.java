package com.pedropacheco.nutrition.backend.userAuth.controller;

import com.pedropacheco.nutrition.backend.userAuth.dto.LoginResponseDTO;
import com.pedropacheco.nutrition.backend.userAuth.dto.UserAuthMapper;
import com.pedropacheco.nutrition.backend.userAuth.service.UserAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAuthController.class)
class UserAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserAuthService userAuthService;

    @MockitoBean
    private UserAuthMapper userAuthMapper;

    @MockitoBean
    private CacheManager cacheManager;

    @Test
    void shouldReturnOkWhenRegisteringValidUser() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO("test@example.com", "Password123!", "user123");

        doNothing().when(userAuthService).registerUser(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnTokenWhenLoginIsSuccessful() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("test@example.com", "Password123!");
        LoginResponseDTO mockResponse = new LoginResponseDTO("mock-jwt-token-for-user123");

        when(userAuthService.authenticate(anyString(), anyString()))
                .thenReturn("mock-jwt-token-for-user123");

        when(userAuthMapper.toDTO(anyString()))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("test@example.com", "WrongPassword!");

        when(userAuthService.authenticate(any(), any()))
                .thenThrow(new BadCredentialsException("Invalid credentials."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    public record RegisterRequestDTO(String email, String password, String userId) {}
    public record LoginRequestDTO(String identifier, String password) {}
}
