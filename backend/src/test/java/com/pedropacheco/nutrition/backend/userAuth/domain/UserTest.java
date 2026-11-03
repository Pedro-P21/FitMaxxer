package com.pedropacheco.nutrition.backend.userAuth.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateUserSuccessfullyWithValidValueObjects() {
        Email email = new Email("test@example.com");
        Password password = new Password("Password123!");
        UserId userId = new UserId("user_01");

        User user = new User(email, password, userId);

        assertEquals(email, user.getEmail());
        assertEquals(userId, user.getUserId());
        assertNotNull(user.getPassword());
    }

    @Test
    void shouldThrowExceptionWhenRequiredAttributesAreNull() {
        Email email = new Email("test@example.com");
        Password password = new Password("Password123!");

        assertThrows(IllegalArgumentException.class, () -> new User(null, password, new UserId("user_01")));
        assertThrows(IllegalArgumentException.class, () -> new User(email, null, new UserId("user_01")));
        assertThrows(IllegalArgumentException.class, () -> new User(email, password, null));
    }
}