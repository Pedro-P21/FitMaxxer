package com.pedropacheco.nutrition.backend.userAuth.service;

import com.pedropacheco.nutrition.backend.userAuth.domain.Email;
import com.pedropacheco.nutrition.backend.userAuth.domain.Password;
import com.pedropacheco.nutrition.backend.userAuth.domain.User;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import com.pedropacheco.nutrition.backend.userAuth.factory.UserFactory;
import com.pedropacheco.nutrition.backend.userAuth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserFactory userFactory;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserAuthService userAuthService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Email email = new Email("test@example.com");
        Password password = new Password("hashed_password_123");
        UserId userId = new UserId("user123");
        mockUser = new User(email, password, userId);
    }


    @Test
    void shouldRegisterUserSuccessfully() {
        // Arrange
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed_password_123");
        when(userFactory.createNewUser(anyString(), anyString(), anyString())).thenReturn(mockUser);
        when(userRepository.existsByEmailOrUserId(any(Email.class), any(UserId.class))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act & Assert
        assertDoesNotThrow(() -> userAuthService.registerUser("test@example.com", "Password123!", "user123"));

        // Verify
        verify(passwordEncoder, times(1)).encode("Password123!");
        verify(userFactory, times(1)).createNewUser("test@example.com", "hashed_password_123", "user123");
        verify(userRepository, times(1)).existsByEmailOrUserId(any(Email.class), any(UserId.class));
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void shouldThrowExceptionWhenRegisteringExistingUser() {
        // Arrange
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password_123");
        when(userFactory.createNewUser(anyString(), anyString(), anyString())).thenReturn(mockUser);
        when(userRepository.existsByEmailOrUserId(any(Email.class), any(UserId.class))).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userAuthService.registerUser("test@example.com", "Password123!", "user123")
        );

        assertEquals("User with given email or ID already exists.", exception.getMessage());

        // Verify que nunca tentou gravar na BD se o utilizador já existe
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void shouldAuthenticateSuccessfullyAndReturnToken() {
        // Arrange
        when(userRepository.findByIdentifier("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("Password123!", "hashed_password_123")).thenReturn(true);

        // Act
        String token = userAuthService.authenticate("test@example.com", "Password123!");

        // Assert
        assertNotNull(token);
        assertEquals("mock-jwt-token-for-user123", token);
        verify(userRepository, times(1)).findByIdentifier("test@example.com");
        verify(passwordEncoder, times(1)).matches("Password123!", "hashed_password_123");
    }

    @Test
    void shouldThrowBadCredentialsWhenUserNotFound() {
        // Arrange
        when(userRepository.findByIdentifier("unknown@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> userAuthService.authenticate("unknown@example.com", "Password123!")
        );

        assertEquals("Invalid credentials.", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void shouldThrowBadCredentialsWhenPasswordDoesNotMatch() {
        // Arrange
        when(userRepository.findByIdentifier("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("WrongPassword!", "hashed_password_123")).thenReturn(false);

        // Act & Assert
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> userAuthService.authenticate("test@example.com", "WrongPassword!")
        );

        assertEquals("Invalid credentials.", exception.getMessage());
        verify(userRepository, times(1)).findByIdentifier("test@example.com");
        verify(passwordEncoder, times(1)).matches("WrongPassword!", "hashed_password_123");
    }

}