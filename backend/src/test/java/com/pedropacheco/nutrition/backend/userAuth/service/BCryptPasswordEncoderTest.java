package com.pedropacheco.nutrition.backend.userAuth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordEncoderTest {

    @Test
    void shouldStoreOneWayHashAndVerifyOnlyTheMatchingPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "DevelopmentPassword123!";

        String encodedPassword = encoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encoder.matches(rawPassword, encodedPassword));
        assertFalse(encoder.matches("DifferentPassword123!", encodedPassword));
        assertNotEquals(encodedPassword, encoder.encode(rawPassword));
    }
}
