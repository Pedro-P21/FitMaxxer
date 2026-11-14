package com.pedropacheco.nutrition.backend.dailyLog.domain;

import com.pedropacheco.nutrition.backend.dailyLog.service.NutrientScalingService;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "daily_log_meal")
public class Meal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "meal_type", nullable = false))
    private MealType mealType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_log_id", nullable = false)
    private DailyLog dailyLog;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "meal_id", nullable = false)
    @OrderBy("id ASC")
    private List<MealItem> items = new ArrayList<>();

    protected Meal() {}
    Meal(MealType mealType, DailyLog dailyLog) {
        this.mealType = Objects.requireNonNull(mealType);
        this.dailyLog = Objects.requireNonNull(dailyLog);
    }

    void addItem(MealItem item) {
        Objects.requireNonNull(item, "Meal item cannot be null.");
        if (containsItem(item)) throw new IllegalStateException("Item already belongs to this meal.");
        items.add(item);
    }
    boolean containsItem(MealItem candidate) {
        return items.stream().anyMatch(item -> item == candidate || candidate.getId() != null
                && candidate.getId().equals(item.getId()));
    }

    void removeItem(MealItem item) {
        if (!items.remove(item)) throw new IllegalStateException("Item does not belong to this meal.");
    }

    public MealType getMealType() { return mealType; }
    public List<MealItem> getItems() { return Collections.unmodifiableList(items); }

    public NutrientTotals calculateNutrientTotals(NutrientScalingService scalingService) {
        NutrientTotals totals = NutrientTotals.ZERO;
        for (MealItem item : items) {
            totals = totals.plus(scalingService.calculateContribution(item.getFoodSnapshot(), item.getPortionQuantity()));
        }
        return totals;
    }
}
