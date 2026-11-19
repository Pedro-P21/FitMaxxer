import 'nutrition.dart';

class CatalogFood {
  CatalogFood.fromJson(Map<String, dynamic> json)
    : id = json['foodId'] as String,
      name = json['name'] as String,
      brand = json['brand'] as String?,
      barcode = json['barcode'] as String?,
      nutrition = PortionNutrition(
        nutrients: Nutrients.fromJson(json),
        basis: json['nutrientBasis'] as String,
        servingQuantity: (json['servingQuantity'] as num?)?.toDouble(),
        servingUnit: json['servingUnit'] as String?,
      );
  final String id, name;
  final String? brand, barcode;
  final PortionNutrition nutrition;
}
