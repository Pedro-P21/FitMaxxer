package com.pedropacheco.nutrition.backend.dailyLog.domain;

import com.pedropacheco.nutrition.backend.dailyLog.Exception.LoggedItemNotFoundException;
import com.pedropacheco.nutrition.backend.dailyLog.service.NutrientScalingService;
import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "daily_log", uniqueConstraints = @UniqueConstraint(
        name = "uk_daily_log_user_date", columnNames = {"user_id", "log_date"}))
public class DailyLog {
    @EmbeddedId
    private DailyLogId id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "log_date", nullable = false, updatable = false))
    private LogDate date;

    @Column(name = "profile_id", nullable = false, updatable = false)
    private Long profileId;

    @Embedded
    private DailyGoalSnapshot goalSnapshot;

    @OneToMany(mappedBy = "dailyLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Meal> meals = new ArrayList<>();

    protected DailyLog() {}

    public DailyLog(UserId userId, LocalDate date, Long profileId, DailyGoalSnapshot goalSnapshot) {
        this(DailyLogId.generate(), userId, date, profileId, goalSnapshot);
    }

    public DailyLog(DailyLogId id, UserId userId, LocalDate date, Long profileId, DailyGoalSnapshot goalSnapshot) {
        this.id = Objects.requireNonNull(id, "Daily log ID cannot be null.");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null.").getValue();
        this.date = new LogDate(date);
        if (profileId == null || profileId <= 0) throw new IllegalArgumentException("Profile ID must be positive.");
        this.profileId = profileId;
        this.goalSnapshot = Objects.requireNonNull(goalSnapshot, "Goal snapshot cannot be null.");
    }

    public void addMealItem(MealType mealType, MealItem item) {
        Objects.requireNonNull(mealType, "Meal type cannot be null.");
        Objects.requireNonNull(item, "Meal item cannot be null.");
        Meal targetMeal = "SNACKS".equals(mealType.getValue()) ? null
                : meals.stream().filter(meal -> meal.getMealType().equals(mealType)).findFirst().orElse(null);
        if (targetMeal == null) {
            targetMeal = new Meal(mealType, this);
            meals.add(targetMeal);
        }
        targetMeal.addItem(item);
    }

    public MealItem getItem(Long itemId) {
        if (itemId == null || itemId <= 0) throw new IllegalArgumentException("Item ID must be positive.");
        return meals.stream().flatMap(meal -> meal.getItems().stream())
                .filter(item -> itemId.equals(item.getId())).findFirst()
                .orElseThrow(LoggedItemNotFoundException::new);
    }

    public DailyNutritionSummary calculateNutritionSummary(NutrientScalingService scalingService) {
        Objects.requireNonNull(scalingService, "Nutrient scaling service cannot be null.");
        NutrientTotals consumed = NutrientTotals.ZERO;
        for (Meal meal : meals) {
            consumed = consumed.plus(meal.calculateNutrientTotals(scalingService));
        }
        return DailyNutritionSummary.from(consumed, goalSnapshot);
    }

    /** Refreshes today's target snapshot after the user's active goals change. */
    public void updateGoalSnapshot(DailyGoalSnapshot goalSnapshot) {
        this.goalSnapshot = Objects.requireNonNull(goalSnapshot, "Goal snapshot cannot be null.");
    }

    public DailyLogId getId() { return id; }
    public UserId getUserId() { return new UserId(userId); }
    public LogDate getDate() { return date; }
    public Long getProfileId() { return profileId; }
    public DailyGoalSnapshot getGoalSnapshot() { return goalSnapshot; }
    public List<Meal> getMeals() { return Collections.unmodifiableList(meals); }
}
