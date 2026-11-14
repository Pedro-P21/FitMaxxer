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

    @Transient
    private boolean claimedForLogging;

    protected MealItem() {}
    public MealItem(FoodSnapshot foodSnapshot, PortionQuantity portionQuantity) {
        this.foodSnapshot = Objects.requireNonNull(foodSnapshot, "Food snapshot cannot be null.");
        this.portionQuantity = Objects.requireNonNull(portionQuantity, "Portion quantity cannot be null.");
    }

    void claimForLogging() {
        if (id != null || claimedForLogging)
            throw new IllegalStateException("Only a new, unassigned item can be added. Move existing items instead.");
        claimedForLogging = true;
    }

    public Long getId() { return id; }

    public void replacePortionQuantity(PortionQuantity replacement) {
        this.portionQuantity = Objects.requireNonNull(replacement, "Portion quantity cannot be null.");
    }

    public FoodSnapshot getFoodSnapshot() { return foodSnapshot; }
    public PortionQuantity getPortionQuantity() { return portionQuantity; }
}
