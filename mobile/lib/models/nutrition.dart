/// Catalog and saved-log nutrients retain their measurement basis until scaled.
class Nutrients {
  const Nutrients({
    this.calories = 0,
    this.protein = 0,
    this.carbs = 0,
    this.fat = 0,
  });
  factory Nutrients.fromJson(Map<String, dynamic> json) => Nutrients(
    calories: (json['caloriesKcal'] as num?)?.toDouble() ?? 0,
    protein: (json['proteinGrams'] as num?)?.toDouble() ?? 0,
    carbs: (json['carbohydrateGrams'] as num?)?.toDouble() ?? 0,
    fat: (json['fatGrams'] as num?)?.toDouble() ?? 0,
  );
  final double calories, protein, carbs, fat;
  Nutrients scaled(double factor) => Nutrients(
    calories: calories * factor,
    protein: protein * factor,
    carbs: carbs * factor,
    fat: fat * factor,
  );
  Nutrients operator +(Nutrients n) => Nutrients(
    calories: calories + n.calories,
    protein: protein + n.protein,
    carbs: carbs + n.carbs,
    fat: fat + n.fat,
  );
  Nutrients operator -(Nutrients n) => this + n.scaled(-1);
}

String amountText(num value) =>
    value.toStringAsFixed(4).replaceFirst(RegExp(r'\.?0+$'), '');
String unitLabel(String unit, {bool short = false}) => switch (unit) {
  'GRAM' => short ? 'g' : 'grams',
  'MILLILITER' => short ? 'ml' : 'milliliters',
  'SERVING' => 'servings',
  _ => unit.toLowerCase(),
};

/// Shared preview rules mirror NutrientScalingService; the API remains authoritative.
class PortionNutrition {
  const PortionNutrition({
    required this.nutrients,
    required this.basis,
    required this.servingQuantity,
    required this.servingUnit,
  });
  final Nutrients nutrients;
  final String basis;
  final double? servingQuantity;
  final String? servingUnit;
  static const _grams = {
    'g': 1.0,
    'gram': 1.0,
    'grams': 1.0,
    'kg': 1000.0,
    'mg': .001,
  };
  static const _milliliters = {
    'ml': 1.0,
    'milliliter': 1.0,
    'milliliters': 1.0,
    'millilitre': 1.0,
    'millilitres': 1.0,
    'l': 1000.0,
    'liter': 1000.0,
    'liters': 1000.0,
    'cl': 10.0,
    'dl': 100.0,
  };
  String? get measuredServingUnit {
    final unit = servingUnit?.trim().toLowerCase();
    if (_grams.containsKey(unit)) return 'GRAM';
    if (_milliliters.containsKey(unit)) return 'MILLILITER';
    return null;
  }

  double? get measuredServingAmount {
    if (servingQuantity == null || servingQuantity! <= 0) return null;
    final factor =
        _grams[servingUnit?.trim().toLowerCase()] ??
        _milliliters[servingUnit?.trim().toLowerCase()];
    return factor == null ? null : servingQuantity! * factor;
  }

  List<String> get units {
    final requiredUnit = basis == 'PER_100_GRAMS'
        ? 'GRAM'
        : basis == 'PER_100_MILLILITERS'
        ? 'MILLILITER'
        : null;
    if (requiredUnit != null) {
      return [
        requiredUnit,
        if (measuredServingUnit == requiredUnit &&
            measuredServingAmount != null)
          'SERVING',
      ];
    }
    if (basis == 'PER_SERVING') {
      return [
        'SERVING',
        if (measuredServingUnit != null && measuredServingAmount != null)
          measuredServingUnit!,
      ];
    }
    return [];
  }

  Nutrients? forQuantity(double quantity, String unit) {
    if (!quantity.isFinite || quantity <= 0 || !units.contains(unit)) {
      return null;
    }
    double factor;
    if (basis == 'PER_SERVING') {
      factor = unit == 'SERVING' ? quantity : quantity / measuredServingAmount!;
    } else {
      factor =
          (unit == 'SERVING' ? quantity * measuredServingAmount! : quantity) /
          100;
    }
    return nutrients.scaled(factor);
  }

  String get basisLabel => switch (basis) {
    'PER_100_GRAMS' => '100 g',
    'PER_100_MILLILITERS' => '100 ml',
    _ => 'serving',
  };
}

/// The log database accepts up to 8 integer and 4 fractional decimal digits.
String? quantityError(String? text, {double multiplier = 1}) {
  final normalized = (text ?? '').trim().replaceAll(',', '.');
  final amount = double.tryParse(normalized);
  if (amount == null || !amount.isFinite || amount <= 0) {
    return 'Enter a quantity greater than zero.';
  }
  if (!RegExp(r'^\d+(\.\d{1,4})?$').hasMatch(normalized)) {
    return 'Use up to 4 decimal places.';
  }
  final scaled = amount * multiplier;
  if (scaled >= 100000000) return 'Quantity must be below 100,000,000.';
  return null;
}
