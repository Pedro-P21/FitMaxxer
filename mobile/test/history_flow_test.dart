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
import 'package:mobile/widgets/app_button.dart';

import 'logging_fixtures.dart';
import 'session_provider_test.dart' show MemoryTokenStore, savedProfile, token;

void main() {
  late ApiClient api;
  late SessionProvider session;
  late List<http.Request> calls;
  late Map<String, dynamic> today;
  var historyFail = false, copyFail = false, empty = false;
  Completer<http.Response>? pendingHistory, pendingCopy;
  late Map<String, dynamic> source;

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
              home: TodayView(session: session),
            ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('History'));
    await tester.pumpAndSettle();
  }

  Future<void> toCopy(WidgetTester tester) async {
    await tester.tap(find.byKey(const ValueKey('history-day-2026-11-15')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('copy-source-Breakfast')));
    await tester.pumpAndSettle();
  }

  setUp(() {
    calls = [];
    historyFail = copyFail = empty = false;
    pendingHistory = pendingCopy = null;
    today = {...diaryJson(quantity: 50, meal: 'Dinner'), 'date': '2026-11-19'};
    source = {
      ...diaryJson(quantity: 150, meal: 'Breakfast', second: true),
      'date': '2026-11-15',
    };
    api = ApiClient(
      client: MockClient((r) async {
        calls.add(r);
        if (r.url.path == '/api/profiles') {
          return http.Response(jsonEncode(savedProfile), 200);
        }
        if (r.url.path.endsWith('/history')) {
          if (pendingHistory != null) return pendingHistory!.future;
          if (historyFail) return http.Response('{}', 503);
          final month = int.parse(r.url.queryParameters['month']!);
          final year = int.parse(r.url.queryParameters['year']!);
          return http.Response(
            jsonEncode({
              'year': year,
              'month': month,
              'logs': empty || month != 11 || year != 2026 ? [] : [source],
            }),
            200,
          );
        }
        if (r.url.path.endsWith('/meal-copies')) {
          if (pendingCopy != null) return pendingCopy!.future;
          if (copyFail) {
            return http.Response('{"detail":"Could not copy this meal."}', 409);
          }
          // An authoritative server response containing existing and copied entries.
          today = {
            ...diaryJson(quantity: 50, meal: 'Dinner'),
            'date': '2026-11-19',
          };
          final dinner = (today['meals'] as List).lastWhere(
            (m) => m['type'] == 'Dinner',
          );
          dinner['items'] = [
            yogurtItem(quantity: 50),
            yogurtItem(quantity: 150, id: 2),
            yogurtItem(quantity: 100, id: 3),
          ];
          dinner['consumed'] = nutrients(300, 36, 12, 12);
          today['nutritionSummary'] = {
            'consumed': nutrients(300, 36, 12, 12),
            'targets': nutrients(2200, 140, 275, 60),
            'remaining': nutrients(1900, 104, 263, 48),
          };
        }
        return http.Response(jsonEncode(today), 200);
      }),
    )..token = token();
    session = SessionProvider(api: api, store: MemoryTokenStore(api.token));
  });

  testWidgets(
    'read-only history copies whole meal into selected destination and returns to Today',
    (tester) async {
      await open(tester);
      expect(find.text('Your food journal'), findsOneWidget);
      await tester.tap(find.byKey(const ValueKey('history-day-2026-11-15')));
      await tester.pumpAndSettle();
      expect(find.text('Read-only day'), findsOneWidget);
      expect(
        find.byKey(const ValueKey('copy-source-Breakfast')),
        findsOneWidget,
      );
      expect(find.byKey(const ValueKey('copy-source-Dinner')), findsNothing);
      expect(find.text('Edit food'), findsNothing);
      await tester.tap(find.byKey(const ValueKey('copy-source-Breakfast')));
      await tester.pumpAndSettle();
      expect(find.text('Copy meal to Breakfast'), findsOneWidget);
      expect(find.text('2 items · 250 kcal'), findsOneWidget);
      await tester.tap(find.byKey(const ValueKey('copy-destination-Dinner')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Copy meal to Dinner'));
      await tester.pumpAndSettle();
      expect(find.text('Breakfast copied to Dinner'), findsOneWidget);
      expect(find.text('Your food journal'), findsNothing);
      expect(find.text('1900 kcal remaining'), findsOneWidget);
      expect(
        tester
            .widget<Text>(find.byKey(const ValueKey('meal-total-Dinner')))
            .data,
        '300',
      );
      final write = calls.singleWhere((r) => r.method == 'POST');
      expect(jsonDecode(write.body), {
        'sourceDate': '2026-11-15',
        'sourceMealType': 'Breakfast',
        'destinationMealType': 'Dinner',
      });
      expect((source['meals'] as List).first['items'], hasLength(2));
      await tester.tap(find.text('History'));
      await tester.pumpAndSettle();
      await toCopy(tester);
      await tester.tap(find.byKey(const ValueKey('copy-destination-Dinner')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Copy meal to Dinner'));
      await tester.pumpAndSettle();
      expect(calls.where((r) => r.method == 'POST'), hasLength(2));
      expect(find.text('Breakfast copied to Dinner'), findsOneWidget);
    },
  );

  testWidgets('month selection, empty history label and picker bounds', (
    tester,
  ) async {
    await open(tester);
    await tester.tap(find.text('November 2026 ⌄'));
    await tester.pumpAndSettle();
    final december = tester.widget<AppButton>(
      find.widgetWithText(AppButton, 'Dec'),
    );
    expect(december.onPressed, isNull);
    expect(find.text('2027'), findsNothing);
    await tester.tap(find.text('Oct'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('View October 2026'));
    await tester.pumpAndSettle();
    expect(find.text('No logs here yet'), findsOneWidget);
    expect(find.text('Choose another date'), findsOneWidget);
    expect(find.text('Choose another month'), findsNothing);
    await tester.tap(find.text('Choose another date'));
    await tester.pumpAndSettle();
    expect(find.text('Choose a month'), findsOneWidget);
    await tester.tap(find.byTooltip('Earlier years'));
    await tester.pumpAndSettle();
    expect(find.text('View October 2025'), findsOneWidget);
    // Canceling the picker does not fetch or change the selected month.
    final requests = calls.length;
    Navigator.of(tester.element(find.text('Choose a month'))).pop();
    await tester.pumpAndSettle();
    expect(find.text('October 2026 ⌄'), findsOneWidget);
    expect(calls, hasLength(requests));
  });

  testWidgets('history loading is shown until the response arrives', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
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
    pendingHistory = Completer<http.Response>();
    await tester.tap(find.text('History'));
    await tester.pump();
    expect(find.text('Loading your food journal…'), findsOneWidget);
    expect(find.text('No logs here yet'), findsNothing);
    pendingHistory!.complete(
      http.Response('{"year":2026,"month":11,"logs":[]}', 200),
    );
    await tester.pumpAndSettle();
    expect(find.text('No logs here yet'), findsOneWidget);
  });

  testWidgets('history failure retries without showing empty history', (
    tester,
  ) async {
    historyFail = true;
    await open(tester);
    expect(find.text('Your journal couldn’t load'), findsOneWidget);
    expect(find.text('No logs here yet'), findsNothing);
    historyFail = false;
    await tester.tap(find.text('Try again'));
    await tester.pumpAndSettle();
    expect(
      find.byKey(const ValueKey('history-day-2026-11-15')),
      findsOneWidget,
    );
  });

  testWidgets('failed copy retains destination and source, retry succeeds', (
    tester,
  ) async {
    await open(tester);
    await toCopy(tester);
    await tester.tap(find.byKey(const ValueKey('copy-destination-Dinner')));
    await tester.pumpAndSettle();
    copyFail = true;
    await tester.tap(find.text('Copy meal to Dinner'));
    await tester.pumpAndSettle();
    expect(find.text('Copy meal to Dinner'), findsOneWidget);
    expect(find.text('Breakfast copied to Dinner'), findsNothing);
    await tester.scrollUntilVisible(
      find.text('Could not copy this meal.'),
      100,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.text('Could not copy this meal.'), findsOneWidget);
    copyFail = false;
    await tester.tap(find.text('Copy meal to Dinner'));
    await tester.pumpAndSettle();
    expect(find.text('Breakfast copied to Dinner'), findsOneWidget);
  });

  testWidgets(
    'pending copy blocks repeat taps and updates only after success',
    (tester) async {
      await open(tester);
      await toCopy(tester);
      pendingCopy = Completer<http.Response>();
      await tester.tap(find.text('Copy meal to Breakfast'));
      await tester.pump();
      final button = tester.widget<AppButton>(find.byType(AppButton).last);
      expect(button.isLoading, isTrue);
      expect(calls.where((r) => r.method == 'POST'), hasLength(1));
      pendingCopy!.complete(http.Response(jsonEncode(today), 200));
      await tester.pumpAndSettle();
      expect(find.text('Breakfast copied to Breakfast'), findsOneWidget);
    },
  );

  testWidgets('session expiry removes copy and past-day routes', (
    tester,
  ) async {
    await open(tester, root: true);
    await toCopy(tester);
    await session.expire();
    await tester.pumpAndSettle();
    expect(find.text('Your session has expired.'), findsOneWidget);
    expect(find.text('Bring a meal forward'), findsNothing);
  });

  testWidgets('Profile and History tabs connect and preserve selected month', (
    tester,
  ) async {
    await open(tester);
    await tester.tap(find.text('November 2026 ⌄'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Oct'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('View October 2026'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Profile'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('History'));
    await tester.pumpAndSettle();
    expect(find.text('October 2026 ⌄'), findsOneWidget);
  });
}
