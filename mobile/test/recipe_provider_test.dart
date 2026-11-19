import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/models/catalog_food.dart';
import 'package:mobile/models/recipe.dart';
import 'package:mobile/models/daily_log.dart';
import 'package:mobile/providers/daily_log_provider.dart';
import 'package:mobile/providers/recipes_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/daily_log_service.dart';
import 'package:mobile/services/recipe_service.dart';

import 'logging_fixtures.dart';
import 'recipe_fixtures.dart';

RecipeDraft draft() => RecipeDraft()
  ..name = 'Oats bowl'
  ..servings = '2'
  ..ingredients.addAll([
    DraftIngredient(
      food: CatalogFood.fromJson(oatsFood),
      quantity: '80',
      unit: 'GRAM',
    ),
    DraftIngredient(
      food: CatalogFood.fromJson(milkFood),
      quantity: '200',
      unit: 'MILLILITER',
    ),
  ]);
void main() {
  test('draft previews mixed units, fractional yield and immutable saved snapshots', () {
    final d = draft();
    expect(d.total!.calories, 432);
    expect(d.perServing!.protein, closeTo(7.8, .00001));
    d.servings = '0.5';
    expect(d.perServing!.calories, 864);
    d.servings = '0';
    expect(d.perServing, isNull);
    d.ingredients[0].unit = 'MILLILITER';
    expect(d.total, isNull);
    final saved = RecipeDetails.fromJson(recipeJson);
    expect(saved.ingredients[0].contribution!.calories, 304);
    expect(saved.perServing.scaled(.5).calories, 108);
  });
  test(
    'create sends catalog references and quantities, not frontend nutrition',
    () async {
      late http.Request call;
      final api = ApiClient(
        client: MockClient((r) async {
          call = r;
          return http.Response(jsonEncode(recipeJson), 201);
        }),
      )..token = 'test';
      final recipes = RecipesProvider(RecipeService(api));
      addTearDown(() {
        recipes.dispose();
        api.close();
      });
      final saved = await recipes.create(draft());
      expect(call.url.path, '/api/recipes');
      expect(call.method, 'POST');
      expect(jsonDecode(call.body), {
        'name': 'Oats bowl',
        'numberOfServings': 2.0,
        'ingredients': [
          {'foodId': 'oats', 'quantity': 80.0, 'quantityUnit': 'GRAM'},
          {'foodId': 'milk', 'quantity': 200.0, 'quantityUnit': 'MILLILITER'},
        ],
      });
      expect(saved!.perServing.calories, 216);
      expect(recipes.dirty, isTrue);
    },
  );
  test(
    'pagination and retry use explicit pages and preserve existing recipes',
    () async {
      var fail = false;
      final calls = <http.Request>[];
      final api = ApiClient(
        client: MockClient((r) async {
          calls.add(r);
          return fail
              ? http.Response('{}', 503)
              : http.Response(
                  jsonEncode(
                    recipePage(
                      page: int.parse(r.url.queryParameters['page']!),
                      totalPages: 2,
                    ),
                  ),
                  200,
                );
        }),
      )..token = 'test';
      final recipes = RecipesProvider(RecipeService(api));
      addTearDown(() {
        recipes.dispose();
        api.close();
      });
      await recipes.load();
      expect(recipes.hasMore, isTrue);
      fail = true;
      await recipes.load(more: true);
      expect(recipes.items, hasLength(1));
      expect(recipes.error, isNotNull);
      expect(recipes.page, 0);
      fail = false;
      await recipes.load(more: recipes.retryMore);
      expect(calls.map((r) => r.url.queryParameters['page']), ['0', '1', '1']);
      expect(recipes.items, hasLength(1));
      expect(recipes.hasMore, isFalse);
    },
  );
  test(
    'duplicate saves are blocked; late list results cannot hide a creation',
    () async {
      final pending = Completer<http.Response>(),
          list = Completer<http.Response>();
      var writes = 0;
      final api = ApiClient(
        client: MockClient((r) {
          if (r.method == 'POST') {
            writes++;
            return pending.future;
          }
          return list.future;
        }),
      )..token = 'test';
      final recipes = RecipesProvider(RecipeService(api));
      addTearDown(() {
        recipes.dispose();
        api.close();
      });
      final loading = recipes.load();
      final first = recipes.create(draft());
      expect(await recipes.create(draft()), isNull);
      pending.complete(http.Response(jsonEncode(recipeJson), 201));
      await first;
      list.complete(http.Response(jsonEncode(recipePage(empty: true)), 200));
      await loading;
      expect(writes, 1);
      expect(recipes.dirty, isTrue);
      expect(recipes.loading, isFalse);
    },
  );
  test(
    'recipe logging accepts fractional servings and retains Today on failure',
    () async {
      var fail = true;
      late http.Request call;
      final api = ApiClient(
        client: MockClient((r) async {
          call = r;
          return fail
              ? http.Response('{}', 503)
              : http.Response(jsonEncode(diaryJson(quantity: 108)), 200);
        }),
      )..token = 'test';
      final diary = DailyLogProvider(DailyLogService(api))
        ..log = DailyLog.fromJson(diaryJson());
      addTearDown(() {
        diary.dispose();
        api.close();
      });
      final before = diary.log;
      expect(
        await diary.addRecipe('recipe-1', 'Oats bowl', 'Breakfast', .5),
        isFalse,
      );
      expect(diary.log, same(before));
      fail = false;
      expect(
        await diary.addRecipe('recipe-1', 'Oats bowl', 'Breakfast', .5),
        isTrue,
      );
      expect(call.url.path, '/api/daily-log/today/recipe-items');
      expect(jsonDecode(call.body), {
        'recipeId': 'recipe-1',
        'mealType': 'Breakfast',
        'quantity': .5,
      });
      expect(diary.log!.consumed.calories, 108);
    },
  );
}
