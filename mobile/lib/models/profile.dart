import 'goal_option.dart';

const activityLabels = {
  'SEDENTARY': 'Sedentary',
  'LIGHTLY_ACTIVE': 'Lightly active',
  'MODERATELY_ACTIVE': 'Moderately active',
  'VERY_ACTIVE': 'Very active',
  'EXTRA_ACTIVE': 'Extra active',
};
const activityDescriptions = {
  'SEDENTARY': 'Little or no regular exercise',
  'LIGHTLY_ACTIVE': 'Exercise 1–3 days a week',
  'MODERATELY_ACTIVE': 'Exercise 3–5 days a week',
  'VERY_ACTIVE': 'Exercise 6–7 days a week',
  'EXTRA_ACTIVE': 'Very demanding exercise or physical work',
};

class ProfileDraft {
  ProfileDraft({
    this.age = '',
    this.weightKg = '',
    this.heightCm = '',
    this.bodyFat = '',
    this.gender = 'MALE',
    this.activity = 'SEDENTARY',
    this.goal = GoalOption.maintenance,
  });
  String age, weightKg, heightCm, bodyFat, gender, activity;
  GoalOption goal;

  Map<String, dynamic> toJson() => {
    'age': int.parse(age), 'weight': double.parse(weightKg),
    'height': double.parse(heightCm), 'gender': gender,
    'bodyFatPercentage': bodyFat.isEmpty ? null : double.parse(bodyFat),
    'activityLevel': activity, 'goalType': goal.apiValue,
    // Measurements are canonical metric values even when entered in imperial.
    'unitSystem': 'METRIC',
  };
}

class UserProfile {
  UserProfile.fromJson(Map<String, dynamic> json)
    : userId = json['userId'] as String?,
      age = (json['age'] as num).toInt(),
      weightKg = (json['weightKg'] as num).toDouble(),
      heightCm = (json['heightCm'] as num).toDouble(),
      gender = json['gender'] as String,
      bodyFat = (json['bodyFatPercentage'] as num?)?.toDouble(),
      activity = json['activityLevel'] as String,
      maintenance = (json['maintenanceCalories'] as num).toInt(),
      calories = (json['calorieTarget'] as num).toInt(),
      protein = (json['proteinGrams'] as num).toInt(),
      carbs = (json['carbsGrams'] as num).toInt(),
      fat = (json['fatsGrams'] as num).toInt();
  final int age, maintenance, calories, protein, carbs, fat;
  final double weightKg, heightCm;
  final double? bodyFat;
  final String gender, activity;
  final String? userId;

  Map<String, dynamic> toJson() => {
    'userId': userId,
    'age': age,
    'weightKg': weightKg,
    'heightCm': heightCm,
    'gender': gender,
    'bodyFatPercentage': bodyFat,
    'activityLevel': activity,
    'maintenanceCalories': maintenance,
    'calorieTarget': calories,
    'proteinGrams': protein,
    'carbsGrams': carbs,
    'fatsGrams': fat,
  };

  GoalOption? get goal {
    for (final option in GoalOption.values) {
      if (option.offset == calories - maintenance) return option;
    }
    return null;
  }

  ProfileDraft toDraft() => ProfileDraft(
    age: '$age',
    weightKg: '$weightKg',
    heightCm: '$heightCm',
    bodyFat: bodyFat == null ? '' : '$bodyFat',
    gender: gender,
    activity: activity,
    goal: GoalOption.values.firstWhere(
      (g) => g.offset == calories - maintenance,
      orElse: () => GoalOption.maintenance,
    ),
  );
}
