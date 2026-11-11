package com.pedropacheco.nutrition.backend.dailyLog.domain;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "daily_log_meal_item")
public class MealItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private FoodSnapshot foodSnapshot;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "serving_quantity", nullable = false, precision = 12, scale = 4))
    private PortionQuantity portionQuantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    protected MealItem() {}
    public MealItem(FoodSnapshot foodSnapshot, PortionQuantity portionQuantity) {
        this.foodSnapshot = Objects.requireNonNull(foodSnapshot, "Food snapshot cannot be null.");
        this.portionQuantity = Objects.requireNonNull(portionQuantity, "Portion quantity cannot be null.");
    }

    void attachTo(Meal meal) {
        if (this.meal != null) throw new IllegalStateException("A meal item can only belong to one meal.");
        this.meal = Objects.requireNonNull(meal);
    }
    public FoodSnapshot getFoodSnapshot() { return foodSnapshot; }
    public PortionQuantity getPortionQuantity() { return portionQuantity; }
}
