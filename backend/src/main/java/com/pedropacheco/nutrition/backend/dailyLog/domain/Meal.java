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

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<MealItem> items = new ArrayList<>();

    protected Meal() {}
    Meal(MealType mealType, DailyLog dailyLog) {
        this.mealType = Objects.requireNonNull(mealType);
        this.dailyLog = Objects.requireNonNull(dailyLog);
    }

    void addItem(MealItem item) {
        Objects.requireNonNull(item, "Meal item cannot be null.");
        item.attachTo(this);
        items.add(item);
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
