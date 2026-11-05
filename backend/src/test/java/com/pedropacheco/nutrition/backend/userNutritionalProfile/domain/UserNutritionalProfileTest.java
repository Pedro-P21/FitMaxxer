package com.pedropacheco.nutrition.backend.userNutritionalProfile.domain;

import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserNutritionalProfileTest {

    private final UserId userId = new UserId("user-123");
    private final PhysicalData physicalData = PhysicalData.validateAndCreate(30, 70, 175, "MALE", null);
    private final ActivityLevel activityLevel = ActivityLevel.of("MODERATELY_ACTIVE");
    private final NutritionalGoal goal = new NutritionalGoal(1700, 1700, new MacroBreakdown(100, 100, 100));

    @Test
    void createsAggregateWithAllRequiredData() {
        UserNutritionalProfile profile = new UserNutritionalProfile(userId, physicalData, activityLevel, goal);

        assertNull(profile.getId());
        assertEquals(userId, profile.getUserId());
        assertEquals(physicalData, profile.getPhysicalData());
        assertEquals(activityLevel, profile.getActivityLevel());
        assertEquals(goal, profile.getNutritionalGoal());
    }

    @Test
    void rejectsMissingRequiredComponents() {
        assertThrows(IllegalArgumentException.class, () -> new UserNutritionalProfile(null, physicalData, activityLevel, goal));
        assertThrows(IllegalArgumentException.class, () -> new UserNutritionalProfile(userId, null, activityLevel, goal));
        assertThrows(IllegalArgumentException.class, () -> new UserNutritionalProfile(userId, physicalData, null, goal));
        assertThrows(IllegalArgumentException.class, () -> new UserNutritionalProfile(userId, physicalData, activityLevel, null));
    }

    @Test
    void sameAggregateInstanceIsEqualToItself() {
        UserNutritionalProfile profile = new UserNutritionalProfile(userId, physicalData, activityLevel, goal);

        assertEquals(profile, profile);
    }
}
