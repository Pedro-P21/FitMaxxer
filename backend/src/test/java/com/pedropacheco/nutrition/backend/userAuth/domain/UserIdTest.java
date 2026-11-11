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
    void userIdsWithTheSameValueAreEqual() {
        assertEquals(new UserId("pedro123"), new UserId(" pedro123 "));
        assertEquals(new UserId("pedro123").hashCode(), new UserId("pedro123").hashCode());
    }

    @Test
    void shouldRejectNullOrBlankUserId() {
        assertThrows(IllegalArgumentException.class, () -> new UserId(null));
        assertThrows(IllegalArgumentException.class, () -> new UserId("   "));
    }
}
