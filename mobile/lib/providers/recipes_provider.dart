import 'package:flutter/foundation.dart';

import '../models/recipe.dart';
import '../services/api_client.dart';
import '../services/recipe_service.dart';

class RecipesProvider extends ChangeNotifier {
  RecipesProvider(this.service);
  final RecipeService service;
  List<RecipeSummary> items = [];
  bool loading = false,
      loaded = false,
      saving = false,
      dirty = false,
      _disposed = false;
  int page = -1, totalPages = 0, _request = 0;
  String? error, saveError;
  bool retryMore = false;
  bool get hasMore => page + 1 < totalPages;
  @override
  void notifyListeners() {
    if (!_disposed) super.notifyListeners();
  }

  Future<void> load({bool more = false}) async {
    if (_disposed || saving || more && (loading || !hasMore)) return;
    final request = ++_request, target = more ? page + 1 : 0;
    retryMore = more;
    loading = true;
    error = null;
    notifyListeners();
    try {
      final result = await service.list(target);
      if (_disposed || request != _request) return;
      items = more
          ? {
              ...{for (final r in items) r.id: r},
              ...{for (final r in result.items) r.id: r},
            }.values.toList()
          : result.items;
      page = result.page;
      totalPages = result.totalPages;
      loaded = true;
      dirty = false;
    } catch (e) {
      if (!_disposed && request == _request) {
        error = e is ApiException
            ? e.message
            : 'Your recipes could not be loaded. Please try again.';
      }
    } finally {
      if (!_disposed && request == _request) {
        loading = false;
        notifyListeners();
      }
    }
  }

  void clearSaveError() {
    saveError = null;
  }

  Future<RecipeDetails?> create(RecipeDraft draft) async {
    if (saving || _disposed) return null;
    saving = true;
    saveError = null;
    _request++;
    loading = false;
    notifyListeners();
    try {
      final result = await service.create(draft);
      if (_disposed) return null;
      dirty = true;
      return result;
    } catch (e) {
      if (!_disposed) {
        saveError = e is ApiException
            ? e.message
            : 'Your recipe could not be saved. Please try again.';
      }
      return null;
    } finally {
      saving = false;
      notifyListeners();
    }
  }

  @override
  void dispose() {
    _disposed = true;
    _request++;
    super.dispose();
  }
}
