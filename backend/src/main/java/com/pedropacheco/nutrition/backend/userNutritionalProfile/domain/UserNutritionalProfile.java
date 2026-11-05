package com.pedropacheco.nutrition.backend.userNutritionalProfile.domain;

import com.pedropacheco.nutrition.backend.userAuth.domain.UserId;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "user_nutritional_profile")
public class UserNutritionalProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "userid", nullable = false, unique = true))
    private UserId userId;

    @Embedded
    private PhysicalData physicalData;

    @Embedded
    private ActivityLevel activityLevel;

    @Embedded
    private NutritionalGoal nutritionalGoal;

    protected UserNutritionalProfile() {}

    public UserNutritionalProfile(UserId userId, PhysicalData physicalData, ActivityLevel activityLevel, NutritionalGoal nutritionalGoal) {
        if (userId == null) throw new IllegalArgumentException("UserId cannot be null");
        if (physicalData == null) throw new IllegalArgumentException("PhysicalData cannot be null");
        if (activityLevel == null) throw new IllegalArgumentException("ActivityLevel cannot be null");
        if (nutritionalGoal == null) throw new IllegalArgumentException("NutritionalGoal cannot be null");

        this.userId = userId;
        this.physicalData = physicalData;
        this.activityLevel = activityLevel;
        this.nutritionalGoal = nutritionalGoal;
    }

    public Long getId() { return id; }
    public UserId getUserId() { return userId; }
    public PhysicalData getPhysicalData() { return physicalData; }
    public ActivityLevel getActivityLevel() { return activityLevel; }
    public NutritionalGoal getNutritionalGoal() { return nutritionalGoal; }

    public void updateData(PhysicalData physicalData, ActivityLevel activityLevel, NutritionalGoal nutritionalGoal) {
        if (physicalData == null) throw new IllegalArgumentException("PhysicalData cannot be null");
        if (activityLevel == null) throw new IllegalArgumentException("ActivityLevel cannot be null");
        if (nutritionalGoal == null) throw new IllegalArgumentException("NutritionalGoal cannot be null");

        this.physicalData = physicalData;
        this.activityLevel = activityLevel;
        this.nutritionalGoal = nutritionalGoal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserNutritionalProfile that = (UserNutritionalProfile) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
