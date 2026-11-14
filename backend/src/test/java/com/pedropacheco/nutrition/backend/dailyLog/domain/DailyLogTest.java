package com.pedropacheco.nutrition.backend.dailyLog.domain;

import com.pedropacheco.nutrition.backend.dailyLog.service.NutrientScalingService;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class DailyLogTest {
    private final NutrientScalingService scalingService = new NutrientScalingService();

    @Test
    void calculatesConsumedAndRemainingNutritionFromMealItemsAndGoalSnapshot() {
        DailyLog log = new DailyLog(new UserId("user-1"), LocalDate.of(2026, 11, 10), 1L,
                new DailyGoalSnapshot(2500, 150, 250, 80));
        log.addMealItem(new MealType("Breakfast"), item("banana", "100", "2", "3", "0.5"));
        log.addMealItem(new MealType("Lunch"), item("milk", "100", "4", "5", "3"));

        DailyNutritionSummary summary = log.calculateNutritionSummary(scalingService);

        assertEquals(new BigDecimal("200"), summary.consumed().caloriesKcal());
        assertEquals(new BigDecimal("6"), summary.consumed().proteinGrams());
        assertEquals(new BigDecimal("8"), summary.consumed().carbohydrateGrams());
        assertEquals(new BigDecimal("3.5"), summary.consumed().fatGrams());
        assertEquals(new BigDecimal("2300"), summary.remaining().caloriesKcal());
        assertEquals(new BigDecimal("144"), summary.remaining().proteinGrams());
    }

    @Test
    void scalesPerHundredGramNutritionByCatalogServingAndQuantity() {
        FoodSnapshot food = new FoodSnapshot("Oats", null, null,
                new BigDecimal("380"), new BigDecimal("13"), new BigDecimal("68"), new BigDecimal("7"),
                NutrientComposition.Basis.PER_100_GRAMS, new BigDecimal("40"), "g");

        NutrientTotals contribution = scalingService.calculateContribution(food, new PortionQuantity("1.5"));

        assertEquals(0, new BigDecimal("228").compareTo(contribution.caloriesKcal()));
        assertEquals(0, new BigDecimal("7.8").compareTo(contribution.proteinGrams()));
    }

    @Test
    void scalesPerHundredMilliliterNutritionFromCatalogServingOrDirectVolume() {
        FoodSnapshot milk = new FoodSnapshot("Milk", null, null,
                new BigDecimal("35"), new BigDecimal("3.5"), new BigDecimal("5"), new BigDecimal("0.1"),
                NutrientComposition.Basis.PER_100_MILLILITERS, new BigDecimal("250"), "ml");

        NutrientTotals oneCatalogServing = scalingService.calculateContribution(milk,
                new PortionQuantity(BigDecimal.ONE, PortionQuantity.Unit.SERVING));
        NutrientTotals threeHundredMilliliters = scalingService.calculateContribution(milk,
                PortionQuantity.milliliters(new BigDecimal("300")));

        assertEquals(0, new BigDecimal("87.5").compareTo(oneCatalogServing.caloriesKcal()));
        assertEquals(0, new BigDecimal("105").compareTo(threeHundredMilliliters.caloriesKcal()));
    }

    @Test
    void scalesNamedFoodPortionWhenItsFoodSpecificEquivalentIsKnown() {
        FoodSnapshot soup = new FoodSnapshot("Soup", null, null,
                new BigDecimal("60"), new BigDecimal("2"), new BigDecimal("8"), new BigDecimal("1"),
                NutrientComposition.Basis.PER_100_MILLILITERS, new BigDecimal("100"), "ml");
        PortionQuantity bowl = PortionQuantity.named("bowl", BigDecimal.ONE, new BigDecimal("250"),
                PortionQuantity.Unit.MILLILITER);

        NutrientTotals contribution = scalingService.calculateContribution(soup, bowl);

        assertEquals(0, new BigDecimal("150").compareTo(contribution.caloriesKcal()));
    }

    @Test
    void canRepresentNamedPortionAsCatalogServingCountWhenNutritionIsPerServing() {
        FoodSnapshot bottle = new FoodSnapshot("Protein Drink", null, null,
                new BigDecimal("180"), new BigDecimal("20"), new BigDecimal("15"), new BigDecimal("3"),
                NutrientComposition.Basis.PER_SERVING, new BigDecimal("330"), "ml");
        PortionQuantity twoBottles = PortionQuantity.named("bottle", new BigDecimal("2"), BigDecimal.ONE,
                PortionQuantity.Unit.SERVING);

        NutrientTotals contribution = scalingService.calculateContribution(bottle, twoBottles);

        assertEquals(0, new BigDecimal("360").compareTo(contribution.caloriesKcal()));
    }

    @Test
    void convertsCatalogServingUnitsWithinTheSameDimension() {
        FoodSnapshot flour = new FoodSnapshot("Flour", null, null,
                new BigDecimal("364"), new BigDecimal("10"), new BigDecimal("76"), new BigDecimal("1"),
                NutrientComposition.Basis.PER_100_GRAMS, new BigDecimal("0.25"), "kg");

        NutrientTotals contribution = scalingService.calculateContribution(flour,
                new PortionQuantity(BigDecimal.ONE, PortionQuantity.Unit.SERVING));

        assertEquals(0, new BigDecimal("910").compareTo(contribution.caloriesKcal()));
    }

    @Test
    void rejectsUnsupportedMealTypesAndNonPositiveQuantities() {
        assertThrows(IllegalArgumentException.class, () -> new MealType("Brunch"));
        assertThrows(IllegalArgumentException.class, () -> new PortionQuantity("0"));
        assertThrows(IllegalArgumentException.class, () -> new PortionQuantity("-1"));
    }

    @Test
    void reusesOneMealForEveryTypeIncludingSnacksAndRejectsDuplicateMembership() {
        var log = new DailyLog(new UserId("user-1"), LocalDate.of(2026, 11, 14), 1L,
                new DailyGoalSnapshot(2000, 120, 220, 70));
        for (String type : new String[]{"Breakfast", "Lunch", "Dinner", "Snacks"}) {
            var entry = item("Food", "100", "2", "3", "1");
            log.addMealItem(new MealType(type), entry);
            log.addMealItem(new MealType(type), item("Food", "100", "2", "3", "1"));
            assertThrows(IllegalStateException.class, () -> log.addMealItem(new MealType("Snacks"), entry));
        }
        var otherLog = new DailyLog(new UserId("user-2"), LocalDate.of(2026, 11, 14), 2L,
                new DailyGoalSnapshot(2000, 120, 220, 70));
        assertThrows(IllegalStateException.class, () -> otherLog.addMealItem(new MealType("Dinner"),
                log.getMeals().getFirst().getItems().getFirst()));
        assertEquals(4, log.getMeals().size());
        assertTrue(log.getMeals().stream().allMatch(meal -> meal.getItems().size() == 2));
        assertThrows(UnsupportedOperationException.class, () -> log.getMeals().getFirst().getItems().clear());
    }

    private MealItem item(String name, String calories, String protein, String carbs, String fat) {
        return new MealItem(new FoodSnapshot(name, null, null,
                new BigDecimal(calories), new BigDecimal(protein), new BigDecimal(carbs), new BigDecimal(fat),
                NutrientComposition.Basis.PER_SERVING, BigDecimal.ONE, "serving"), new PortionQuantity("1"));
    }
}
