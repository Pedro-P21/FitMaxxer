import 'nutrition.dart';

const mealTypes = ['Breakfast', 'Lunch', 'Dinner', 'Snacks'];

class LoggedItem {
  LoggedItem.fromJson(Map<String, dynamic> json)
    : id = (json['itemId'] as num).toInt(),
      name = json['foodName'] as String,
      quantity = (json['quantity'] as num).toDouble(),
      unit = json['quantityUnit'] as String? ?? 'SERVING',
      portionLabel = json['portionLabel'] as String?,
      nutrition = PortionNutrition(
        nutrients: Nutrients.fromJson(json),
        basis: json['nutrientBasis'] as String,
        servingQuantity: (json['catalogServingQuantity'] as num?)?.toDouble(),
        servingUnit: json['catalogServingUnit'] as String?,
      );
  final int id;
  final String name, unit;
  final String? portionLabel;
  final double quantity;
  final PortionNutrition nutrition;
  Nutrients? get consumed => nutrition.forQuantity(quantity, unit);
  String get portion =>
      '${amountText(quantity)} ${portionLabel ?? unitLabel(unit, short: true)}';
}

class LoggedMeal {
  LoggedMeal({required this.type, required this.items, required this.consumed});
  factory LoggedMeal.fromJson(Map<String, dynamic> json) => LoggedMeal(
    type: json['type'] as String,
    items: (json['items'] as List)
        .map((i) => LoggedItem.fromJson(i as Map<String, dynamic>))
        .toList(),
    consumed: Nutrients.fromJson(
      json['consumed'] as Map<String, dynamic>? ?? {},
    ),
  );
  final String type;
  final List<LoggedItem> items;
  final Nutrients consumed;
}

class DailyLog {
  DailyLog.fromJson(Map<String, dynamic> json)
    : date = DateTime.parse(json['date'] as String),
      meals = (json['meals'] as List)
          .map((m) => LoggedMeal.fromJson(m as Map<String, dynamic>))
          .toList(),
      consumed = Nutrients.fromJson(
        json['nutritionSummary']['consumed'] as Map<String, dynamic>,
      ),
      targets = Nutrients.fromJson(
        json['nutritionSummary']['targets'] as Map<String, dynamic>,
      ),
      remaining = Nutrients.fromJson(
        json['nutritionSummary']['remaining'] as Map<String, dynamic>,
      );
  final DateTime date;
  final List<LoggedMeal> meals;
  final Nutrients consumed, targets, remaining;
  LoggedMeal meal(String type) => meals.firstWhere(
    (m) => m.type.toLowerCase() == type.toLowerCase(),
    orElse: () =>
        LoggedMeal(type: type, items: [], consumed: const Nutrients()),
  );
  LoggedItem? item(int id) {
    for (final meal in meals) {
      for (final item in meal.items) {
        if (item.id == id) return item;
      }
    }
    return null;
  }

  String? mealFor(int id) {
    for (final meal in meals) {
      if (meal.items.any((i) => i.id == id)) return meal.type;
    }
    return null;
  }
}
