# Nutritional Calculation Specification

This document defines the mathematical algorithms and domain logic used by the `NutritionalGoalCalculatorService` to calculate Basal Metabolic Rate (BMR), Total Daily Energy Expenditure (TDEE) / Maintenance Calories, and final Calorie/Macro Targets based on user metrics and goals.

---

## 1. Basal Metabolic Rate (BMR) Calculation

The foundation of energy expenditure estimation relies on the clinically validated **Mifflin-St Jeor Equation**. Calculations use normalized metric units (weight in kilograms, height in centimeters, and age in years).

### Formulas by Gender:

* **For Men:**
  $$\text{BMR} = (10 \times \text{weight}) + (6.25 \times \text{height}) - (5 \times \text{age}) + 5$$

* **For Women:**
  $$\text{BMR} = (10 \times \text{weight}) + (6.25 \times \text{height}) - (5 \times \text{age}) - 161$$

---

## 2. Total Daily Energy Expenditure (TDEE) / Maintenance Calories

TDEE represents the total number of calories a user burns daily, including resting metabolism and physical activity. It is calculated by multiplying the BMR by an **Activity Factor**:

$$\text{Maintenance Calories (TDEE)} = \text{BMR} \times \text{Activity Factor}$$

### Activity Multipliers:
* **Sedentary** (little to no exercise, desk job): $\text{BMR} \times 1.2$
* **Lightly Active** (light exercise/sports 1–3 days/week): $\text{BMR} \times 1.375$
* **Moderately Active** (moderate exercise/sports 3–5 days/week): $\text{BMR} \times 1.55$
* **Very Active** (hard exercise 6–7 days/week): $\text{BMR} \times 1.725$
* **Extra Active** (very hard physical job or 2x daily training): $\text{BMR} \times 1.9$

---

## 3. Calorie Target Adjustment (Goal Type)

Once maintenance calories are established, the final daily caloric target (`CalorieTarget`) is adjusted according to the user's selected goal type (`GoalType` enum):

$$\text{CalorieTarget} = \text{TDEE} + \text{Goal Adjustment Offset}$$

### Goal Adjustments:
* **Aggressive Cutting:** $\text{TDEE} - 500 \text{ kcal}$
* **Moderate Cutting:** $\text{TDEE} - 300 \text{ kcal}$
* **Light Cutting:** $\text{TDEE} - 200 \text{ kcal}$
* **Maintain Weight:** $\text{TDEE} + 0 \text{ kcal}$
* **Light Bulk:** $\text{TDEE} + 200 \text{ kcal}$
* **Moderate Bulk:** $\text{TDEE} + 300 \text{ kcal}$
* **Aggressive Bulk:** $\text{TDEE} + 500 \text{ kcal}$

---
