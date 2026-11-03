package com.pedropacheco.nutrition.backend.userAuth.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserIdTest {

    @Test
    void shouldCreateValidUserId() {
        UserId userId = new UserId("pedro123");
        assertEquals("pedro123", userId.getValue());
    }

    @Test
    void shouldRejectNullOrBlankUserId() {
        assertThrows(IllegalArgumentException.class, () -> new UserId(null));
        assertThrows(IllegalArgumentException.class, () -> new UserId("   "));
    }
}