package com.pedropacheco.nutrition.backend.userNutritionalProfile.domain;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class PhysicalData {

    private int age;
    private double weightKg;
    private double heightCm;
    private String gender;
    private Double bodyFatPercentage;

    protected PhysicalData() {

    }

    private PhysicalData(int age, double weightKg, double heightCm, String gender, Double bodyFatPercentage) {
        validateInvariants(age, weightKg, heightCm, gender, bodyFatPercentage);
        this.age = age;
        this.weightKg = weightKg;
        this.heightCm = heightCm;
        this.gender = gender.toUpperCase();
        this.bodyFatPercentage = bodyFatPercentage;
    }

    public static PhysicalData validateAndCreate(int age, double weightKg, double heightCm, String gender, Double bodyFatPercentage) {
        return new PhysicalData(age, weightKg, heightCm, gender, bodyFatPercentage);
    }

    private void validateInvariants(int age, double weightKg, double heightCm, String gender, Double bodyFat) {
        if (age <= 0 || age > 120) {
            throw new IllegalArgumentException("Age must be between 1 and 120 years.");
        }
        if (weightKg <= 10 || weightKg > 635) {
            throw new IllegalArgumentException("Weight must be between 20 kg and 635 kg.");
        }
        if (heightCm <= 100 || heightCm > 250) {
            throw new IllegalArgumentException("Height must be between 100 cm and 250 cm.");
        }
        if (gender == null || (!gender.equalsIgnoreCase("MALE") && !gender.equalsIgnoreCase("FEMALE"))) {
            throw new IllegalArgumentException("Gender must be either MALE or FEMALE.");
        }
        if (bodyFat != null && (bodyFat < 3.0 || bodyFat > 80.0)) {
            throw new IllegalArgumentException("Body fat percentage must be between 3% and 80%.");
        }
    }

    public int getAge() { return age; }
    public double getWeightKg() { return weightKg; }
    public double getHeightCm() { return heightCm; }
    public String getGender() { return gender; }
    public Double getBodyFatPercentage() { return bodyFatPercentage; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhysicalData that = (PhysicalData) o;
        return age == that.age &&
                Double.compare(that.weightKg, weightKg) == 0 &&
                Double.compare(that.heightCm, heightCm) == 0 &&
                Objects.equals(gender, that.gender) &&
                Objects.equals(bodyFatPercentage, that.bodyFatPercentage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(age, weightKg, heightCm, gender, bodyFatPercentage);
    }
}