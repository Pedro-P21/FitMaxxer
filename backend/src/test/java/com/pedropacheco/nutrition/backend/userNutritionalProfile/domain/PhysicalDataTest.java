package com.pedropacheco.nutrition.backend.userNutritionalProfile.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhysicalDataTest {

    @Test
    void createsPhysicalDataAndNormalizesGender() {
        PhysicalData data = PhysicalData.validateAndCreate(30, 70.5, 175.0, "female", null);

        assertEquals(30, data.getAge());
        assertEquals(70.5, data.getWeightKg());
        assertEquals(175.0, data.getHeightCm());
        assertEquals("FEMALE", data.getGender());
        assertNull(data.getBodyFatPercentage());
    }

    @Test
    void acceptsInclusiveAgeAndBodyFatBoundsAndUpperPhysicalBounds() {
        PhysicalData data = PhysicalData.validateAndCreate(120, 635.0, 250.0, "MALE", 80.0);
        PhysicalData lowerBodyFat = PhysicalData.validateAndCreate(1, 10.01, 100.01, "MALE", 3.0);

        assertEquals(120, data.getAge());
        assertEquals(80.0, data.getBodyFatPercentage());
        assertEquals(1, lowerBodyFat.getAge());
        assertEquals(3.0, lowerBodyFat.getBodyFatPercentage());
    }

    @Test
    void rejectsAgeOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(0, 70, 175, "MALE", null));
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(121, 70, 175, "MALE", null));
    }

    @Test
    void rejectsWeightAtOrOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(30, 10, 175, "MALE", null));
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(30, 636, 175, "MALE", null));
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(30, Double.NaN, 175, "MALE", null));
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(30, Double.POSITIVE_INFINITY, 175, "MALE", null));
    }

    @Test
    void rejectsHeightAtOrOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(30, 70, 100, "MALE", null));
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(30, 70, 250.01, "MALE", null));
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(30, 70, Double.NaN, "MALE", null));
    }

    @Test
    void rejectsMissingOrUnsupportedGender() {
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(30, 70, 175, null, null));
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(30, 70, 175, "", null));
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(30, 70, 175, "OTHER", null));
    }

    @Test
    void rejectsBodyFatOutsideInclusiveThreeToEightyPercentRange() {
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(30, 70, 175, "MALE", 2.99));
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(30, 70, 175, "MALE", 80.01));
        assertThrows(IllegalArgumentException.class, () -> PhysicalData.validateAndCreate(30, 70, 175, "MALE", Double.NaN));
    }
}
