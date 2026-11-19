import 'catalog_food.dart';
import 'nutrition.dart';

class RecipeSummary {
  RecipeSummary.fromJson(Map<String, dynamic> json)
    : id = json['recipeId'] as String,
      name = json['name'] as String,
      servings = (json['numberOfServings'] as num).toDouble(),
      total = Nutrients.fromJson(
        json['totalNutrition'] as Map<String, dynamic>,
      ),
      perServing = Nutrients.fromJson(
        json['perServingNutrition'] as Map<String, dynamic>,
      );
  final String id, name;
  final double servings;
  final Nutrients total, perServing;
}

class RecipeDetails extends RecipeSummary {
  RecipeDetails.fromJson(super.json)
    : ingredients = (json['ingredients'] as List)
          .map((i) => SavedIngredient.fromJson(i as Map<String, dynamic>))
          .toList(),
      super.fromJson();
  final List<SavedIngredient> ingredients;
}

class SavedIngredient {
  SavedIngredient.fromJson(Map<String, dynamic> json)
    : name = json['foodName'] as String,
      quantity = (json['quantity'] as num).toDouble(),
      unit = json['quantityUnit'] as String,
      nutrition = PortionNutrition(
        nutrients: Nutrients.fromJson(
          json['nutrients'] as Map<String, dynamic>,
        ),
        basis: json['nutrientBasis'] as String,
        servingQuantity: (json['servingQuantity'] as num?)?.toDouble(),
        servingUnit: json['servingUnit'] as String?,
      );
  final String name, unit;
  final double quantity;
  final PortionNutrition nutrition;
  Nutrients? get contribution => nutrition.forQuantity(quantity, unit);
}

class DraftIngredient {
  DraftIngredient({required this.food, this.quantity = '1', String? unit})
    : unit = unit ?? food.nutrition.units.first;
  final CatalogFood food;
  String quantity, unit;
  double get amount =>
      double.tryParse(quantity.trim().replaceAll(',', '.')) ?? 0;
  Nutrients? get contribution => quantityError(quantity) == null
      ? food.nutrition.forQuantity(amount, unit)
      : null;
  Map<String, dynamic> toJson() => {
    'foodId': food.id,
    'quantity': amount,
    'quantityUnit': unit,
  };
}

class RecipeDraft {
  String name = '', servings = '1';
  final List<DraftIngredient> ingredients = [];
  Nutrients? get total {
    if (ingredients.isEmpty || ingredients.any((i) => i.contribution == null)) {
      return null;
    }
    return ingredients.fold(
      const Nutrients(),
      (sum, i) => sum! + i.contribution!,
    );
  }

  Nutrients? get perServing => quantityError(servings) == null
      ? total?.scaled(1 / double.parse(servings.replaceAll(',', '.')))
      : null;
  Map<String, dynamic> toJson() => {
    'name': name.trim(),
    'numberOfServings': double.parse(servings.trim().replaceAll(',', '.')),
    'ingredients': ingredients.map((i) => i.toJson()).toList(),
  };
}

class RecipePage {
  RecipePage.fromJson(Map<String, dynamic> json)
    : items = (json['items'] as List)
          .map((r) => RecipeSummary.fromJson(r as Map<String, dynamic>))
          .toList(),
      page = (json['page'] as num).toInt(),
      totalPages = (json['totalPages'] as num).toInt();
  final List<RecipeSummary> items;
  final int page, totalPages;
}
