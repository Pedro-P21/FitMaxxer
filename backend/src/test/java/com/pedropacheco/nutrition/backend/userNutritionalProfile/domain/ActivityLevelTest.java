package com.pedropacheco.nutrition.backend.userNutritionalProfile.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActivityLevelTest {

    @Test
    void mapsEachSupportedLevelToItsFactor() {
        assertLevel("SEDENTARY", "sedentary", 1.2);
        assertLevel("LIGHTLY_ACTIVE", "Lightly_Active", 1.375);
        assertLevel("MODERATELY_ACTIVE", "moderately_active", 1.55);
        assertLevel("VERY_ACTIVE", "Very_Active", 1.725);
        assertLevel("EXTRA_ACTIVE", "extra_active", 1.9);
    }

    @Test
    void rejectsNullBlankAndUnknownLevel() {
        assertThrows(IllegalArgumentException.class, () -> ActivityLevel.of(null));
        assertThrows(IllegalArgumentException.class, () -> ActivityLevel.of(""));
        assertThrows(IllegalArgumentException.class, () -> ActivityLevel.of("  "));
        assertThrows(IllegalArgumentException.class, () -> ActivityLevel.of("WEEKEND WARRIOR"));
    }

    private void assertLevel(String expectedName, String input, double expectedFactor) {
        ActivityLevel level = ActivityLevel.of(input);
        assertEquals(expectedName, level.getLevel());
        assertEquals(expectedFactor, level.getFactor());
    }
}
