import 'dart:convert';

import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/main.dart';
import 'package:mobile/models/catalog_food.dart';
import 'package:mobile/providers/daily_log_provider.dart';
import 'package:mobile/providers/session_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/daily_log_service.dart';
import 'package:mobile/services/food_catalog_service.dart';
import 'package:mobile/theme/app_theme.dart';
import 'package:mobile/views/food_search_view.dart';
import 'package:mobile/views/food_details_view.dart';
import 'package:mobile/views/today_view.dart';

import 'session_provider_test.dart' show MemoryTokenStore, token, savedProfile;
import 'logging_fixtures.dart';

void main() {
  void phone(WidgetTester tester) {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
  }

  testWidgets('meal swipe opens its catalog without removing the meal', (
    tester,
  ) async {
    phone(tester);
    final api = ApiClient(
      client: MockClient(
        (r) async => http.Response(jsonEncode(diaryJson(quantity: 150)), 200),
      ),
    )..token = token();
    final session = SessionProvider(api: api, store: MemoryTokenStore());
    addTearDown(() {
      session.dispose();
      api.close();
    });
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light,
        home: TodayView(session: session),
      ),
    );
    await tester.pumpAndSettle();
    for (final meal in ['Breakfast', 'Snacks']) {
      final row = find.byKey(ValueKey('swipe-meal-$meal'));
      await tester.ensureVisible(row);
      await tester.pumpAndSettle();
      await tester.drag(find.byType(Scrollable).first, const Offset(0, -120));
      await tester.pumpAndSettle();
      await tester.drag(row, const Offset(-45, 0));
      await tester.pumpAndSettle();
      expect(find.byType(FoodSearchView), findsNothing);
      await tester.drag(row, const Offset(-300, 0));
      await tester.pumpAndSettle();
      expect(
        tester.widget<FoodSearchView>(find.byType(FoodSearchView)).meal,
        meal,
      );
      Navigator.of(tester.element(find.byType(FoodSearchView))).pop();
      await tester.pumpAndSettle();
      expect(row, findsOneWidget);
      expect(find.byKey(const ValueKey('logged-item-1')), findsOneWidget);
    }
  });

  testWidgets(
    'search → log → edit → move → delete final item, with confirmations and totals',
    (tester) async {
      phone(tester);
      double quantity = 0;
      String meal = 'Snacks';
      final calls = <http.Request>[];
      final api = ApiClient(
        client: MockClient((r) async {
          calls.add(r);
          if (r.url.path == '/api/foods') {
            return http.Response(
              jsonEncode({
                'items': [yogurtFood],
              }),
              200,
            );
          }
          if (r.method == 'POST') {
            final body = jsonDecode(r.body);
            expect(body['quantityUnit'], 'GRAM');
            quantity = (body['quantity'] as num).toDouble();
            meal = body['mealType'] as String;
          } else if (r.method == 'PATCH') {
            final body = jsonDecode(r.body);
            if (r.url.path.endsWith('quantity')) {
              quantity = (body['quantity'] as num).toDouble();
            } else {
              meal = body['destinationMealType'] as String;
            }
          } else if (r.method == 'DELETE') {
            quantity = 0;
          }
          return http.Response(
            jsonEncode(diaryJson(quantity: quantity, meal: meal)),
            200,
          );
        }),
      )..token = token();
      final session = SessionProvider(api: api, store: MemoryTokenStore());
      addTearDown(() {
        session.dispose();
        api.close();
      });
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.light,
          home: TodayView(session: session),
        ),
      );
      await tester.pumpAndSettle();
      await tester.scrollUntilVisible(
        find.byTooltip('Add food to Snacks'),
        150,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.drag(find.byType(Scrollable).first, const Offset(0, -200));
      await tester.pumpAndSettle();
      await tester.tap(find.byTooltip('Add food to Snacks'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField), 'Greek');
      await tester.pump(const Duration(seconds: 1));
      await tester.enterText(find.byType(TextField), 'Greek yogurt');
      await tester.pump(const Duration(seconds: 1));
      expect(calls.where((r) => r.url.path == '/api/foods'), isEmpty);
      await tester.pump();
      await tester.tap(find.text('Search'));
      await tester.pumpAndSettle();
      expect(calls.where((r) => r.url.path == '/api/foods'), hasLength(1));
      await tester.tap(find.text('Greek yogurt · Full fat'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField), '1.5');
      await tester.pumpAndSettle();
      expect(find.text('150 kcal'), findsOneWidget);
      await tester.tap(find.text('Add 150 kcal to Snacks'));
      await tester.pumpAndSettle();
      expect(quantity, 150);
      expect(find.text('Added to Snacks · totals updated'), findsOneWidget);
      await tester.scrollUntilVisible(
        find.byKey(const ValueKey('logged-item-1')),
        200,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.ensureVisible(find.byKey(const ValueKey('logged-item-1')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const ValueKey('logged-item-1')));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField), '200');
      await tester.pumpAndSettle();
      final moveButton = tester.widget<TextButton>(
        find.widgetWithText(TextButton, 'Move to another meal'),
      );
      expect(moveButton.onPressed, isNull);
      expect(find.text('200 kcal'), findsOneWidget);
      await tester.tap(find.text('Save quantity'));
      await tester.pumpAndSettle();
      expect(find.text('Quantity saved · 200 g'), findsOneWidget);
      await tester.ensureVisible(find.byKey(const ValueKey('logged-item-1')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const ValueKey('logged-item-1')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Move to another meal'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Dinner'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Move to Dinner'));
      await tester.pumpAndSettle();
      expect(meal, 'Dinner');
      expect(quantity, 200);
      expect(find.text('Moved to Dinner · totals unchanged'), findsOneWidget);
      await tester.ensureVisible(find.byKey(const ValueKey('logged-item-1')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const ValueKey('logged-item-1')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Delete this item'));
      await tester.pumpAndSettle();
      expect(find.text('DAILY TOTAL AFTER DELETING'), findsOneWidget);
      await tester.tap(find.text('Keep item'));
      await tester.pumpAndSettle();
      expect(quantity, 200);
      await tester.tap(find.text('Delete this item'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Delete item'));
      await tester.pumpAndSettle();
      expect(quantity, 0);
      expect(find.text('0 items'), findsOneWidget);
      expect(find.byKey(const ValueKey('logged-item-1')), findsNothing);
      expect(calls.where((r) => r.method == 'POST').length, 1);
      expect(calls.where((r) => r.method == 'DELETE').length, 1);
      await tester.pumpWidget(const SizedBox());
    },
  );

  testWidgets('scan opens prefilled barcode confirmation before lookup', (
    tester,
  ) async {
    phone(tester);
    String? lookedUp;
    final api = ApiClient(
      client: MockClient((r) async {
        lookedUp = r.url.queryParameters['query'];
        return http.Response(
          jsonEncode({
            'items': [yogurtFood],
          }),
          200,
        );
      }),
    )..token = token();
    final diary = DailyLogProvider(DailyLogService(api));
    addTearDown(() {
      diary.dispose();
      api.close();
    });
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light,
        home: FoodSearchView(
          catalog: FoodCatalogService(api),
          diary: diary,
          meal: 'Lunch',
          scanBarcode: (_) async => '05901234123457',
        ),
      ),
    );
    await tester.tap(find.text('Scan barcode'));
    await tester.pumpAndSettle();
    expect(find.text('Confirm your barcode'), findsOneWidget);
    expect(
      tester.widget<TextField>(find.byType(TextField).last).controller!.text,
      '05901234123457',
    );
    expect(lookedUp, isNull);
    await tester.tap(find.text('Find food'));
    await tester.pumpAndSettle();
    expect(lookedUp, '05901234123457');
    expect(find.text('Greek yogurt'), findsOneWidget);
  });

  testWidgets(
    'over-target amounts and each logged item are shown independently',
    (tester) async {
      phone(tester);
      final api = ApiClient(
        client: MockClient(
          (r) async => http.Response(
            jsonEncode(diaryJson(quantity: 2320, second: true)),
            200,
          ),
        ),
      )..token = token();
      final session = SessionProvider(api: api, store: MemoryTokenStore());
      addTearDown(() {
        session.dispose();
        api.close();
      });
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.light,
          home: TodayView(session: session),
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('220 over target'), findsOneWidget);
      final progress = tester.widget<CircularProgressIndicator>(
        find.byKey(const ValueKey('calorie-progress')),
      );
      final overflow = tester.widget<CircularProgressIndicator>(
        find.byKey(const ValueKey('calorie-overflow')),
      );
      expect(progress.value, 1);
      expect(overflow.value, closeTo(.1, .0001));
      expect(
        tester.getRect(find.byKey(const ValueKey('calorie-progress'))),
        tester.getRect(find.byKey(const ValueKey('calorie-overflow'))),
      );
      expect(progress.strokeWidth, overflow.strokeWidth);
      expect(
        tester
            .widget<Text>(find.byKey(const ValueKey('meal-total-Snacks')))
            .data,
        '2420',
      );
      expect(
        find.text('220 kcal over · a fresh start tomorrow'),
        findsOneWidget,
      );
      expect(find.byKey(const ValueKey('logged-item-1')), findsNothing);
      expect(find.byKey(const ValueKey('logged-item-2')), findsNothing);
      await tester.scrollUntilVisible(
        find.byKey(const ValueKey('toggle-meal-Snacks')),
        150,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.drag(find.byType(Scrollable).first, const Offset(0, -180));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const ValueKey('toggle-meal-Snacks')));
      await tester.pumpAndSettle();
      await tester.scrollUntilVisible(
        find.byKey(const ValueKey('logged-item-2')),
        200,
        scrollable: find.byType(Scrollable).first,
      );
      expect(find.byKey(const ValueKey('logged-item-1')), findsOneWidget);
      expect(find.byKey(const ValueKey('logged-item-2')), findsOneWidget);
      final header = tester.getRect(
        find.byKey(const ValueKey('meal-header-Snacks')),
      );
      final firstItem = tester.getRect(
        find.byKey(const ValueKey('logged-item-1')),
      );
      expect(header.bottom, lessThanOrEqualTo(firstItem.top));
      final add = tester.getCenter(find.byTooltip('Add food to Snacks'));
      final total = tester.getCenter(
        find.byKey(const ValueKey('meal-total-Snacks')),
      );
      expect(add.dy, closeTo(total.dy, 1));
      await tester.ensureVisible(
        find.byKey(const ValueKey('toggle-meal-Snacks')),
      );
      await tester.tap(find.byKey(const ValueKey('toggle-meal-Snacks')));
      await tester.pumpAndSettle();
      expect(find.byKey(const ValueKey('logged-item-1')), findsNothing);
      expect(find.byKey(const ValueKey('logged-item-2')), findsNothing);
    },
  );

  testWidgets(
    'failed save retains food details and does not claim food was added',
    (tester) async {
      phone(tester);
      final api = ApiClient(
        client: MockClient((r) async => http.Response('{}', 503)),
      )..token = token();
      final diary = DailyLogProvider(DailyLogService(api));
      addTearDown(() {
        diary.dispose();
        api.close();
      });
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.light,
          home: FoodDetailsView(
            food: CatalogFood.fromJson(yogurtFood),
            diary: diary,
            meal: 'Snacks',
          ),
        ),
      );
      await tester.tap(find.text('Add 100 kcal to Snacks'));
      await tester.pumpAndSettle();
      expect(find.text('Greek yogurt'), findsOneWidget);
      expect(diary.message, isNull);
      expect(diary.log, isNull);
      expect(diary.error, isNotNull);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('session expiry dismisses a nested logging route', (
    tester,
  ) async {
    phone(tester);
    final api = ApiClient(
      client: MockClient(
        (r) async => http.Response(
          jsonEncode(
            r.url.path == '/api/profiles' ? savedProfile : diaryJson(),
          ),
          200,
        ),
      ),
    );
    final session = SessionProvider(api: api, store: MemoryTokenStore(token()));
    addTearDown(() {
      session.dispose();
      api.close();
    });
    await tester.pumpWidget(MyApp(session: session));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Log food'));
    await tester.pumpAndSettle();
    expect(find.text('Find your food'), findsOneWidget);
    await session.expire();
    await tester.pumpAndSettle();
    expect(find.text('Your session has expired.'), findsOneWidget);
    expect(find.text('Find your food'), findsNothing);
    expect(tester.takeException(), isNull);
  });
  testWidgets('catalog empty state clears and connection error can retry', (
    tester,
  ) async {
    phone(tester);
    var status = 200;
    var empty = true;
    final api = ApiClient(
      client: MockClient(
        (r) async => http.Response(
          jsonEncode({
            'items': empty ? [] : [yogurtFood],
          }),
          status,
        ),
      ),
    )..token = token();
    final diary = DailyLogProvider(DailyLogService(api));
    addTearDown(() {
      diary.dispose();
      api.close();
    });
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light,
        home: FoodSearchView(
          catalog: FoodCatalogService(api),
          diary: diary,
          meal: 'Snacks',
        ),
      ),
    );
    await tester.enterText(find.byType(TextField), 'unknown');
    await tester.pump();
    await tester.tap(find.text('Search'));
    await tester.pumpAndSettle();
    expect(find.text('No matching foods'), findsOneWidget);
    await tester.tap(find.text('Clear search'));
    await tester.pumpAndSettle();
    expect(
      tester.widget<TextField>(find.byType(TextField)).controller!.text,
      isEmpty,
    );
    status = 503;
    await tester.enterText(find.byType(TextField), 'yogurt');
    await tester.testTextInput.receiveAction(TextInputAction.search);
    await tester.pumpAndSettle();
    expect(find.text('A small connection hiccup'), findsOneWidget);
    status = 200;
    empty = false;
    await tester.tap(find.text('Try again'));
    await tester.pumpAndSettle();
    expect(find.text('Greek yogurt · Full fat'), findsOneWidget);
  });

  testWidgets('barcode lookup never opens an unrelated catalog product', (
    tester,
  ) async {
    phone(tester);
    final api = ApiClient(
      client: MockClient(
        (r) async => http.Response(
          jsonEncode({
            'items': [yogurtFood],
          }),
          200,
        ),
      ),
    )..token = token();
    final diary = DailyLogProvider(DailyLogService(api));
    addTearDown(() {
      diary.dispose();
      api.close();
    });
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light,
        home: FoodSearchView(
          catalog: FoodCatalogService(api),
          diary: diary,
          meal: 'Snacks',
          scanBarcode: (_) async => '1234567890123',
        ),
      ),
    );
    await tester.tap(find.text('Scan barcode'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Find food'));
    await tester.pumpAndSettle();
    expect(
      find.text('No matching food was found for this barcode.'),
      findsOneWidget,
    );
    expect(find.byType(FoodDetailsView), findsNothing);
  });
}
