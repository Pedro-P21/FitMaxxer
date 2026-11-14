package com.pedropacheco.nutrition.backend.dailyLog.factory;

import com.pedropacheco.nutrition.backend.dailyLog.domain.*;
import com.pedropacheco.nutrition.backend.foodCatalog.domain.NutrientComposition;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MealItemFactoryTest {
    @Test
    void copyCreatesIndependentSnapshotAndPortionValuesForEverySupportedUnit() {
        var factory = new MealItemFactory();
        var snapshot = new FoodSnapshot("Recorded recipe", "Brand", "12345678", new BigDecimal("33.3333333333"),
                new BigDecimal("1.2345678912"), BigDecimal.TEN, BigDecimal.ONE,
                NutrientComposition.Basis.PER_SERVING, BigDecimal.ONE, "serving");
        for (var unit : PortionQuantity.Unit.values()) {
            var portion = unit == PortionQuantity.Unit.NAMED
                    ? PortionQuantity.named("bowl", new BigDecimal("1.25"), new BigDecimal("200"), PortionQuantity.Unit.MILLILITER)
                    : new PortionQuantity(new BigDecimal("1.25"), unit);
            var source = new MealItem(snapshot, portion);
            var copied = factory.copy(source);
            assertNotSame(source, copied);
            assertNull(copied.getId());
            assertEquals(snapshot, copied.getFoodSnapshot());
            assertNotSame(snapshot, copied.getFoodSnapshot());
            assertEquals(portion, copied.getPortionQuantity());
            assertNotSame(portion, copied.getPortionQuantity());
            factory.changeItemQuantity(copied, new BigDecimal("2.5"));
            assertEquals(new BigDecimal("1.25"), source.getPortionQuantity().getAmount());
        }
    }

    @Test
    void replacesQuantityWithoutMutatingOldValueOrSnapshot() {
        for (var unit : new PortionQuantity.Unit[]{PortionQuantity.Unit.SERVING, PortionQuantity.Unit.GRAM,
                PortionQuantity.Unit.MILLILITER, PortionQuantity.Unit.NAMED}) {
            var old = unit == PortionQuantity.Unit.NAMED
                    ? PortionQuantity.named("bowl", BigDecimal.ONE, new BigDecimal("200"), PortionQuantity.Unit.GRAM)
                    : new PortionQuantity(BigDecimal.ONE, unit);
            var snapshot = new FoodSnapshot("Food", null, null, BigDecimal.TEN, BigDecimal.ONE,
                    BigDecimal.ONE, BigDecimal.ONE, NutrientComposition.Basis.PER_SERVING, BigDecimal.ONE, "serving");
            var item = new MealItem(snapshot, old);
            new MealItemFactory().changeItemQuantity(item, new BigDecimal("2.5"));
            assertNotSame(old, item.getPortionQuantity());
            assertEquals(BigDecimal.ONE, old.getAmount());
            assertEquals(new BigDecimal("2.5"), item.getPortionQuantity().getAmount());
            assertEquals(unit, item.getPortionQuantity().getUnit());
            assertEquals(old.getLabel(), item.getPortionQuantity().getLabel());
            assertEquals(old.getEquivalentAmount(), item.getPortionQuantity().getEquivalentAmount());
            assertEquals(old.getEquivalentUnit(), item.getPortionQuantity().getEquivalentUnit());
            assertSame(snapshot, item.getFoodSnapshot());
        }
    }
}
