package com.pedropacheco.nutrition.backend.userNutritionalProfile.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MacroBreakdownTest {

    @Test
    void acceptsZeroAndExposesGrams() {
        MacroBreakdown macros = new MacroBreakdown(0, 120, 50);

        assertEquals(0, macros.getProteinGrams());
        assertEquals(120, macros.getCarbsGrams());
        assertEquals(50, macros.getFatsGrams());
    }

    @Test
    void rejectsNegativeAmountForEveryMacro() {
        assertThrows(IllegalArgumentException.class, () -> new MacroBreakdown(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MacroBreakdown(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new MacroBreakdown(0, 0, -1));
    }

    @Test
    void calculatesGramsFromPercentagesAndRoundsCaloriesBeforeConversion() {
        MacroBreakdown macros = MacroBreakdown.fromPercentages(2000, 30, 40, 30);

        assertEquals(new MacroBreakdown(150, 200, 66), macros);
    }

    @Test
    void acceptsPercentageSumWithinToleranceAndRejectsOutsideTolerance() {
        assertDoesNotThrow(() -> MacroBreakdown.fromPercentages(2000, 30, 40, 30.005));
        assertThrows(IllegalArgumentException.class, () -> MacroBreakdown.fromPercentages(2000, 30, 40, 30.02));
    }

    @Test
    void rejectsEachPercentageOutsideZeroToOneHundredOrNonFinite() {
        assertThrows(IllegalArgumentException.class, () -> MacroBreakdown.fromPercentages(2000, -1, 51, 50));
        assertThrows(IllegalArgumentException.class, () -> MacroBreakdown.fromPercentages(2000, 101, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> MacroBreakdown.fromPercentages(2000, 50, Double.NaN, 50));
        assertThrows(IllegalArgumentException.class, () -> MacroBreakdown.fromPercentages(2000, 50, 50, Double.POSITIVE_INFINITY));
    }
}
