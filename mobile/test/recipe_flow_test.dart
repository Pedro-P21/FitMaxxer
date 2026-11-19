import 'dart:async';
import 'dart:convert';

import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/main.dart';
import 'package:mobile/providers/session_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/theme/app_theme.dart';
import 'package:mobile/views/today_view.dart';
import 'package:mobile/widgets/app_text_field.dart';
import 'package:mobile/widgets/app_button.dart';

import 'logging_fixtures.dart';
import 'recipe_fixtures.dart';
import 'session_provider_test.dart' show MemoryTokenStore, savedProfile, token;

void main() {
  late ApiClient api;
  late SessionProvider session;
  late List<http.Request> calls;
  var existing = false,
      failCreate = false,
      failList = false,
      failLog = false,
      failDetails = false;
  Completer<http.Response>? pendingCreate;
  setUp(() {
    existing = failCreate = failList = failLog = failDetails = false;
    pendingCreate = null;
    calls = [];
    api = ApiClient(
      client: MockClient((r) async {
        calls.add(r);
        if (r.url.path == '/api/profiles') {
          return http.Response(jsonEncode(savedProfile), 200);
        }
        if (r.url.path == '/api/foods') {
          return http.Response(
            jsonEncode({
              'items': [
                r.url.queryParameters['query']!.contains('oat')
                    ? oatsFood
                    : milkFood,
              ],
            }),
            200,
          );
        }
        if (r.url.path == '/api/recipes') {
          if (r.method == 'POST') {
            if (pendingCreate != null) return pendingCreate!.future;
            if (failCreate) {
              return http.Response('{"detail":"Could not save recipe."}', 409);
            }
            existing = true;
            return http.Response(jsonEncode(recipeJson), 201);
          }
          return failList
              ? http.Response('{}', 503)
              : http.Response(jsonEncode(recipePage(empty: !existing)), 200);
        }
        if (r.url.path == '/api/recipes/recipe-1') {
          return failDetails
              ? http.Response('{}', 503)
              : http.Response(jsonEncode(recipeJson), 200);
        }
        if (r.url.path.endsWith('/recipe-items')) {
          if (failLog) return http.Response('{}', 503);
          final body = jsonDecode(r.body);
          final servings = body['quantity'] as num;
          final log = diaryJson(
            quantity: 216 * servings.toDouble(),
            meal: body['mealType'],
          );
          final meal = (log['meals'] as List).firstWhere(
            (m) => m['type'] == body['mealType'],
          );
          meal['items'] = [
            {
              'itemId': 9,
              'foodName': 'Oats bowl',
              'quantity': servings,
              'quantityUnit': 'SERVING',
              'nutrientBasis': 'PER_SERVING',
              'caloriesKcal': 216,
              'proteinGrams': 7.8,
              'carbohydrateGrams': 30,
              'fatGrams': 7.2,
            },
          ];
          return http.Response(jsonEncode(log), 200);
        }
        return http.Response(jsonEncode(diaryJson()), 200);
      }),
    )..token = token();
    session = SessionProvider(api: api, store: MemoryTokenStore(api.token));
  });
  Future<void> open(WidgetTester tester, {bool root = false}) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    addTearDown(() {
      session.dispose();
      api.close();
    });
    await tester.pumpWidget(
      root
          ? MyApp(session: session)
          : MaterialApp(
              theme: AppTheme.light,
              home: TodayView(
                session: session,
                scanBarcode: (_) async => '05901234123457',
              ),
            ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Recipes'));
    await tester.pumpAndSettle();
  }

  Finder field(String label, {int index = 0}) => find.descendant(
    of: find
        .byWidgetPredicate((w) => w is AppTextField && w.label == label)
        .at(index),
    matching: find.byType(TextField),
  );
  Future<void> enter(
    WidgetTester tester,
    String label,
    String text, {
    int index = 0,
  }) async {
    final f = field(label, index: index);
    await tester.ensureVisible(f);
    await tester.enterText(f, text);
    await tester.pumpAndSettle();
  }

  Future<void> tap(WidgetTester tester, String text) async {
    tester.testTextInput.hide();
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text(text).first);
    await tester.tap(find.text(text).first);
    await tester.pumpAndSettle();
  }

  Future<void> addOats(WidgetTester tester) async {
    await tap(tester, '+ Add catalog ingredient');
    await enter(tester, 'Search the food catalog', 'oats');
    final prior = calls.where((r) => r.url.path == '/api/foods').length;
    await tester.pump(const Duration(seconds: 1));
    expect(calls.where((r) => r.url.path == '/api/foods'), hasLength(prior));
    await tap(tester, 'Search');
    await tap(tester, 'Rolled oats · Catalog');
    await enter(tester, 'Quantity', '80');
    await tap(tester, 'Add ingredient');
  }

  Future<void> toYield(WidgetTester tester) async {
    await tap(tester, '+ Create a recipe');
    await enter(tester, 'Recipe name', 'Oats bowl');
    await addOats(tester);
    await tap(tester, 'Continue to yield');
  }

  testWidgets(
    'create from name and barcode, revise draft, save, inspect and log half serving',
    (tester) async {
      await open(tester);
      await tap(tester, '+ Create a recipe');
      await enter(tester, 'Recipe name', 'Oats bowl');
      await addOats(tester);
      await tap(tester, '+ Add catalog ingredient');
      await tap(tester, 'Scan barcode');
      expect(find.text('Confirm your barcode'), findsOneWidget);
      expect(
        calls.where((r) => r.url.queryParameters['query'] == '05901234123457'),
        isEmpty,
      );
      await tap(tester, 'Find food');
      await enter(tester, 'Quantity', '200');
      await tap(tester, 'Add ingredient');
      expect(calls.where((r) => r.method == 'POST'), isEmpty);
      await tap(tester, 'Continue to yield');
      await enter(tester, 'Number of servings', '2');
      expect(find.text('216 kcal'), findsOneWidget);
      expect(find.text('7.8 g'), findsOneWidget);
      await tap(tester, '‹  Create recipe');
      expect(
        tester.widget<TextField>(field('Recipe name')).controller!.text,
        'Oats bowl',
      );
      expect(
        tester.widget<TextField>(field('Quantity', index: 0)).controller!.text,
        '80',
      );
      expect(
        tester.widget<TextField>(field('Quantity', index: 1)).controller!.text,
        '200',
      );
      await enter(tester, 'Quantity', '90');
      await enter(tester, 'Quantity', '80');
      await tap(tester, 'Continue to yield');
      expect(
        tester.widget<TextField>(field('Number of servings')).controller!.text,
        '2',
      );
      await tap(tester, 'Save recipe');
      expect(find.text('Ready for your next meal'), findsOneWidget);
      final payload = jsonDecode(
        calls
            .singleWhere(
              (r) => r.method == 'POST' && r.url.path == '/api/recipes',
            )
            .body,
      );
      expect(payload, {
        'name': 'Oats bowl',
        'numberOfServings': 2.0,
        'ingredients': [
          {'foodId': 'oats', 'quantity': 80.0, 'quantityUnit': 'GRAM'},
          {'foodId': 'milk', 'quantity': 200.0, 'quantityUnit': 'MILLILITER'},
        ],
      });
      await tap(tester, 'View recipe');
      expect(find.text('Rolled oats · 80 g'), findsOneWidget);
      expect(find.text('Whole milk · 200 ml'), findsOneWidget);
      expect(find.text('Save recipe'), findsNothing);
      await tap(tester, 'Log this recipe');
      await enter(tester, 'Number of servings', '0.5');
      expect(find.text('108 kcal'), findsOneWidget);
      expect(find.text('3.9 g'), findsOneWidget);
      await tap(tester, 'Log 0.5 servings · 108 kcal');
      expect(
        find.text('Oats bowl added to Breakfast · totals updated'),
        findsOneWidget,
      );
      expect(find.text('Ready for your next meal'), findsNothing);
      expect(
        jsonDecode(
          calls.lastWhere((r) => r.url.path.endsWith('/recipe-items')).body,
        ),
        {'recipeId': 'recipe-1', 'mealType': 'Breakfast', 'quantity': .5},
      );
    },
  );
  testWidgets(
    'compact cards swipe straight to logging without loading details',
    (tester) async {
      existing = true;
      await open(tester);
      expect(find.text('Breakfast favorite'), findsNothing);
      expect(find.text('Rolled oats'), findsNothing);
      final row = find.byKey(const ValueKey('swipe-recipe-recipe-1'));
      await tester.drag(row, const Offset(-45, 0));
      await tester.pumpAndSettle();
      expect(find.text('A little or a lot?'), findsNothing);
      await tester.drag(row, const Offset(-300, 0));
      await tester.pumpAndSettle();
      expect(find.text('A little or a lot?'), findsOneWidget);
      expect(
        calls.where((r) => r.url.path == '/api/recipes/recipe-1'),
        isEmpty,
      );
      await tap(tester, '‹  Recipe details');
      expect(row, findsOneWidget);
    },
  );
  testWidgets(
    'draft ingredient removal and empty yield validation prevent writes',
    (tester) async {
      await open(tester);
      await tap(tester, '+ Create a recipe');
      await enter(tester, 'Recipe name', 'Oats bowl');
      await addOats(tester);
      await tester.tap(find.byTooltip('Remove Rolled oats'));
      await tester.pumpAndSettle();
      expect(find.text('Rolled oats'), findsNothing);
      await addOats(tester);
      await tap(tester, 'Continue to yield');
      await enter(tester, 'Number of servings', '0');
      await tester.pump(const Duration(seconds: 1));
      expect(calls.where((r) => r.method == 'POST'), isEmpty);
      expect(find.text('Enter a quantity greater than zero.'), findsOneWidget);
    },
  );
  testWidgets(
    'failed creation retains draft and retry uses a single successful save',
    (tester) async {
      await open(tester);
      await toYield(tester);
      failCreate = true;
      await tap(tester, 'Save recipe');
      expect(find.text('Could not save recipe.'), findsOneWidget);
      expect(find.text('Ready for your next meal'), findsNothing);
      failCreate = false;
      await tap(tester, 'Save recipe');
      expect(find.text('Ready for your next meal'), findsOneWidget);
      await tap(tester, 'Back to Recipes');
      expect(
        find.byKey(const ValueKey('view-recipe-recipe-1')),
        findsOneWidget,
      );
    },
  );
  testWidgets('recipe list and details retry independently', (tester) async {
    existing = true;
    failList = true;
    await open(tester);
    expect(
      find.text('The server is unavailable. Please try again.'),
      findsOneWidget,
    );
    failList = false;
    await tap(tester, 'Try again');
    failDetails = true;
    await tester.tap(find.byKey(const ValueKey('view-recipe-recipe-1')));
    await tester.pumpAndSettle();
    expect(
      find.text('The server is unavailable. Please try again.'),
      findsOneWidget,
    );
    failDetails = false;
    await tap(tester, 'Try again');
    expect(find.text('The ingredients'), findsOneWidget);
  });
  testWidgets('failed logging keeps recipe amount and allows retry', (
    tester,
  ) async {
    existing = true;
    await open(tester);
    await tester.drag(
      find.byKey(const ValueKey('swipe-recipe-recipe-1')),
      const Offset(-300, 0),
    );
    await tester.pumpAndSettle();
    await enter(tester, 'Number of servings', '0.5');
    failLog = true;
    await tap(tester, 'Log 0.5 servings · 108 kcal');
    expect(
      tester.widget<TextField>(field('Number of servings')).controller!.text,
      '0.5',
    );
    expect(
      find.text('The server is unavailable. Please try again.'),
      findsOneWidget,
    );
    failLog = false;
    await tap(tester, 'Log 0.5 servings · 108 kcal');
    expect(
      find.text('Oats bowl added to Breakfast · totals updated'),
      findsOneWidget,
    );
  });
  testWidgets('pending recipe save blocks duplicate submissions', (
    tester,
  ) async {
    await open(tester);
    await toYield(tester);
    pendingCreate = Completer<http.Response>();
    await tester.tap(find.text('Save recipe'));
    await tester.pump();
    expect(
      tester.widget<AppButton>(find.byType(AppButton).first).isLoading,
      isTrue,
    );
    expect(
      calls.where((r) => r.method == 'POST' && r.url.path == '/api/recipes'),
      hasLength(1),
    );
    pendingCreate!.complete(http.Response(jsonEncode(recipeJson), 201));
    await tester.pumpAndSettle();
    expect(find.text('Ready for your next meal'), findsOneWidget);
  });

  testWidgets('session expiry closes unsaved recipe draft routes', (
    tester,
  ) async {
    await open(tester, root: true);
    await tap(tester, '+ Create a recipe');
    await session.expire();
    await tester.pumpAndSettle();
    expect(find.text('Your session has expired.'), findsOneWidget);
    expect(find.text('Continue to yield'), findsNothing);
  });
}
