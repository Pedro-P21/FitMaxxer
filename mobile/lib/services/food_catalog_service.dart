import '../models/catalog_food.dart';
import 'api_client.dart';

class CatalogResult {
  const CatalogResult(this.foods, this.message);
  final List<CatalogFood> foods;
  final String? message;
}

class FoodCatalogService {
  FoodCatalogService(this.api);
  final ApiClient api;
  Future<CatalogResult> search(String query) async {
    final data = await api.request(
      'GET',
      '/api/foods?${Uri(queryParameters: {'query': query.trim()}).query}',
    );
    return CatalogResult(
      (data['items'] as List)
          .map((f) => CatalogFood.fromJson(f as Map<String, dynamic>))
          .toList(),
      data['message'] as String?,
    );
  }
}
