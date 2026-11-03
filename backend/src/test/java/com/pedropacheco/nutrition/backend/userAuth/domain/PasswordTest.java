package com.pedropacheco.nutrition.backend.userAuth.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordTest {

    @Test
    void shouldCreateValidPassword() {
        Password password = new Password("Password123!");
        assertNotNull(password.getHashedValue());
    }

    @Test
    void shouldRejectInvalidPassword() {
        assertThrows(IllegalArgumentException.class, () -> new Password(""));
    }
}