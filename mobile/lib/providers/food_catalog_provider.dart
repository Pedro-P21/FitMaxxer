import 'package:flutter/foundation.dart';

import '../models/catalog_food.dart';
import '../services/api_client.dart';
import '../services/food_catalog_service.dart';

class FoodCatalogProvider extends ChangeNotifier {
  FoodCatalogProvider(this.service);
  final FoodCatalogService service;
  List<CatalogFood> foods = [];
  String query = '';
  String? error, notice;
  bool loading = false, searched = false, _disposed = false;
  int _generation = 0;
  @override
  void notifyListeners() {
    if (!_disposed) super.notifyListeners();
  }

  void changeQuery(String value) {
    _generation++;
    query = value.trim();
    error = null;
    notice = null;
    foods = [];
    searched = false;
    loading = false;
    notifyListeners();
  }

  Future<void> search(String value) async {
    if (loading && query == value.trim()) return;
    final generation = ++_generation;
    query = value.trim();
    error = null;
    notice = null;
    foods = [];
    searched = false;
    loading = query.isNotEmpty;
    notifyListeners();
    if (query.isEmpty) return;
    try {
      final result = await service.search(query);
      if (_disposed || generation != _generation) return;
      foods = result.foods;
      notice = result.message;
      searched = true;
    } catch (e) {
      if (!_disposed && generation == _generation) {
        error = e is ApiException
            ? e.message
            : 'The food catalog could not be loaded. Please try again.';
      }
    } finally {
      if (generation == _generation) {
        loading = false;
        notifyListeners();
      }
    }
  }

  @override
  void dispose() {
    _disposed = true;
    _generation++;
    super.dispose();
  }
}
