import 'dart:convert';

import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/main.dart';
import 'package:mobile/providers/session_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/theme/app_theme.dart';
import 'package:mobile/views/profile_view.dart';
import 'package:mobile/widgets/app_text_field.dart';

import 'logging_fixtures.dart';
import 'session_provider_test.dart' show MemoryTokenStore, savedProfile, token;

void main() {
  late SessionProvider session;
  late ApiClient api;
  late MemoryTokenStore store;
  late List<http.Request> calls;
  late Map<String, dynamic> profile;
  var failSave = false;

  setUp(() {
    profile = {...savedProfile, 'userId': 'alexfit'};
    calls = [];
    failSave = false;
    store = MemoryTokenStore(token());
    api = ApiClient(
      client: MockClient((r) async {
        calls.add(r);
        if (r.url.path == '/api/daily-log/today') {
          return http.Response(jsonEncode(diaryJson()), 200);
        }
        if (r.method == 'PUT') {
          if (failSave) {
            return http.Response('{"detail":"Save failed. Try again."}', 503);
          }
          final changes = jsonDecode(r.body) as Map<String, dynamic>;
          profile = {...profile, ...changes};
          if (changes.containsKey('macroBreakdown')) {
            profile.addAll(
              Map<String, dynamic>.from(changes['macroBreakdown'] as Map),
            );
          } else if (changes.containsKey('calorieTarget')) {
            profile.addAll({
              'proteinGrams': 150,
              'carbsGrams': 300,
              'fatsGrams': 66,
            });
          }
          // The actual PUT returns only the updated nutritional goals.
          return http.Response(
            jsonEncode({
              for (final key in [
                'maintenanceCalories',
                'calorieTarget',
                'proteinGrams',
                'carbsGrams',
                'fatsGrams',
              ])
                key: profile[key],
            }),
            200,
          );
        }
        return http.Response(jsonEncode(profile), 200);
      }),
    );
    session = SessionProvider(api: api, store: store);
  });
  void cleanup() => addTearDown(() {
    session.dispose();
    api.close();
  });

  Future<void> open(WidgetTester tester) async {
    cleanup();
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await session.restore();
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light,
        home: ProfileView(session: session, onToday: () {}),
      ),
    );
    await tester.pumpAndSettle();
  }

  Future<void> enter(WidgetTester tester, String label, String value) async {
    final field = find.descendant(
      of: find.byWidgetPredicate((w) => w is AppTextField && w.label == label),
      matching: find.byType(TextField),
    );
    await tester.ensureVisible(field);
    await tester.enterText(field, value);
    await tester.pumpAndSettle();
  }

  Future<void> save(WidgetTester tester) async {
    tester.testTextInput.hide();
    await tester.pumpAndSettle();
    await tester.tap(find.text('Save changes'));
    await tester.pumpAndSettle();
  }

  testWidgets(
    'view and edit physical data using PUT, retaining selected goal',
    (tester) async {
      await open(tester);
      expect(find.text('@alexfit'), findsOneWidget);
      expect(find.text('72 kg · 178 cm'), findsOneWidget);
      expect(find.text('Review session status'), findsNothing);
      await tester.tap(find.text('Edit physical data →'));
      await tester.pumpAndSettle();
      await enter(tester, 'Weight · kg', '74.5');
      await save(tester);
      expect(find.text('74.5 kg · 178 cm'), findsOneWidget);
      expect(find.text('Your profile changes are saved'), findsOneWidget);
      final payload = jsonDecode(
        calls.singleWhere((r) => r.method == 'PUT').body,
      );
      expect(payload['weightKg'], 74.5);
      expect(payload['unitSystem'], 'METRIC');
      expect(payload['goalType'], 'MODERATE_CUTTING');
      expect(payload.containsKey('bodyFatPercentage'), isFalse);
      expect(session.profile!.weightKg, 74.5);
    },
  );

  testWidgets('calorie-only edit leaves macro recalculation to backend', (
    tester,
  ) async {
    await open(tester);
    await tester.tap(find.text('Edit daily targets →'));
    await tester.pumpAndSettle();
    await enter(tester, 'Daily calories · kcal', '2400');
    await save(tester);
    expect(find.text('2400 kcal / day'), findsOneWidget);
    expect(jsonDecode(calls.singleWhere((r) => r.method == 'PUT').body), {
      'calorieTarget': 2400,
    });
    expect(session.profile!.protein, 150);
  });

  testWidgets(
    'manual macros validate before request and submit exact payload',
    (tester) async {
      await open(tester);
      await tester.tap(find.text('Edit daily targets →'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Set macros manually'));
      await tester.pumpAndSettle();
      await enter(tester, 'Protein · g', '0');
      await save(tester);
      expect(calls.where((r) => r.method == 'PUT'), isEmpty);
      expect(
        find.text(
          'Calories from macros must be within 15 kcal of your daily target.',
        ),
        findsOneWidget,
      );
      await enter(tester, 'Protein · g', '201');
      await save(tester);
      expect(jsonDecode(calls.singleWhere((r) => r.method == 'PUT').body), {
        'calorieTarget': 2300,
        'macroBreakdown': {
          'proteinGrams': 201,
          'carbsGrams': 230,
          'fatsGrams': 64,
        },
      });
      expect(find.text('Your profile changes are saved'), findsOneWidget);
    },
  );

  testWidgets('failed update retains edited values and saved session profile', (
    tester,
  ) async {
    await open(tester);
    failSave = true;
    await tester.tap(find.text('Edit daily targets →'));
    await tester.pumpAndSettle();
    await enter(tester, 'Daily calories · kcal', '2400');
    await save(tester);
    expect(session.profile!.calories, 2300);
    expect(find.text('Your profile changes are saved'), findsNothing);
    expect(
      find.text('The server is unavailable. Please try again.'),
      findsOneWidget,
    );
    expect(
      tester.widget<TextField>(find.byType(TextField)).controller!.text,
      '2400',
    );
    failSave = false;
    await save(tester);
    expect(find.text('2400 kcal / day'), findsOneWidget);
  });

  testWidgets('existing body fat cannot be silently cleared', (tester) async {
    profile['bodyFatPercentage'] = 17;
    await open(tester);
    await tester.tap(find.text('Edit physical data →'));
    await tester.pumpAndSettle();
    await enter(tester, 'Body fat · optional (%)', '');
    await save(tester);
    expect(calls.where((r) => r.method == 'PUT'), isEmpty);
    expect(
      find.text('Enter a value; an existing body fat value cannot be cleared.'),
      findsOneWidget,
    );
    expect(session.profile!.bodyFat, 17);
  });

  testWidgets('session expiry removes the profile editor', (tester) async {
    cleanup();
    await tester.pumpWidget(MyApp(session: session));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Profile'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Edit physical data →'));
    await tester.pumpAndSettle();
    await session.expire();
    await tester.pumpAndSettle();
    expect(find.text('Your session has expired.'), findsOneWidget);
    expect(find.text('Save changes'), findsNothing);
    expect(store.value, isNull);
  });

  testWidgets('Profile tab, Today refresh, logout and secure token clearing', (
    tester,
  ) async {
    cleanup();
    await tester.pumpWidget(MyApp(session: session));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Profile'));
    await tester.pumpAndSettle();
    expect(find.text('Your profile'), findsOneWidget);
    await tester.tap(find.text('Today'));
    await tester.pumpAndSettle();
    expect(
      calls.where((r) => r.url.path == '/api/daily-log/today').length,
      greaterThanOrEqualTo(2),
    );
    await tester.tap(find.text('Profile'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('Log out'),
      150,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.tap(find.text('Log out'));
    await tester.pumpAndSettle();
    expect(session.stage, SessionStage.signedOut);
    expect(store.value, isNull);
    expect(api.token, isNull);
    expect(find.text('Your profile'), findsNothing);
  });
}
