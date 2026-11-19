Map<String, dynamic> nutrients(
  double calories, [
  double protein = 0,
  double carbs = 0,
  double fat = 0,
]) => {
  'caloriesKcal': calories,
  'proteinGrams': protein,
  'carbohydrateGrams': carbs,
  'fatGrams': fat,
};
const yogurtFood = {
  'foodId': 'openfoodfacts:05901234123457',
  'name': 'Greek yogurt',
  'brand': 'Full fat',
  'barcode': '05901234123457',
  'servingQuantity': 150,
  'servingUnit': 'g',
  'nutrientBasis': 'PER_100_GRAMS',
  'caloriesKcal': 100,
  'proteinGrams': 12,
  'carbohydrateGrams': 4,
  'fatGrams': 4,
};
Map<String, dynamic> yogurtItem({double quantity = 150, int id = 1}) => {
  'itemId': id,
  'foodName': 'Greek yogurt',
  'brand': 'Full fat',
  'quantity': quantity,
  'quantityUnit': 'GRAM',
  'catalogServingQuantity': 150,
  'catalogServingUnit': 'g',
  'nutrientBasis': 'PER_100_GRAMS',
  'caloriesKcal': 100,
  'proteinGrams': 12,
  'carbohydrateGrams': 4,
  'fatGrams': 4,
};
Map<String, dynamic> diaryJson({
  double quantity = 0,
  String meal = 'Snacks',
  bool second = false,
}) => {
  'date': '2026-11-18',
  'meals': [
    for (final type in ['Breakfast', 'Lunch', 'Dinner', 'Snacks'])
      {
        'type': type,
        'items': type == meal && quantity > 0
            ? [
                yogurtItem(quantity: quantity),
                if (second) yogurtItem(quantity: 100, id: 2),
              ]
            : [],
        'consumed': nutrients(type == meal ? quantity + (second ? 100 : 0) : 0),
      },
  ],
  'nutritionSummary': {
    'consumed': nutrients(
      quantity + (second ? 100 : 0),
      quantity * .12 + (second ? 12 : 0),
      quantity * .04 + (second ? 4 : 0),
      quantity * .04 + (second ? 4 : 0),
    ),
    'targets': nutrients(2200, 140, 275, 60),
    'remaining': nutrients(
      2200 - quantity - (second ? 100 : 0),
      140 - quantity * .12 - (second ? 12 : 0),
      275 - quantity * .04 - (second ? 4 : 0),
      60 - quantity * .04 - (second ? 4 : 0),
    ),
  },
};
