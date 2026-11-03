package com.pedropacheco.nutrition.backend.userAuth.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void shouldCreateValidEmail() {
        Email email = new Email("user@example.com");
        assertEquals("user@example.com", email.getEmail());
    }

    @Test
    void shouldRejectInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () -> new Email("invalid-email"));
    }
}