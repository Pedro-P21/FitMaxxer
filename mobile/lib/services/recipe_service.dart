import '../models/recipe.dart';
import 'api_client.dart';

class RecipeService {
  RecipeService(this.api);
  final ApiClient api;
  Future<RecipePage> list(int page) async => RecipePage.fromJson(
    await api.request('GET', '/api/recipes?page=$page&size=20'),
  );
  Future<RecipeDetails> get(String id) async => RecipeDetails.fromJson(
    await api.request('GET', '/api/recipes/${Uri.encodeComponent(id)}'),
  );
  Future<RecipeDetails> create(RecipeDraft draft) async =>
      RecipeDetails.fromJson(
        await api.request('POST', '/api/recipes', body: draft.toJson()),
      );
}
