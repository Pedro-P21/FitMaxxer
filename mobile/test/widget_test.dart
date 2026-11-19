import 'dart:convert';

import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/main.dart';
import 'package:mobile/providers/session_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/views/onboarding_view.dart';

import 'session_provider_test.dart' show MemoryTokenStore, token, savedProfile;

void main() {
  testWidgets('Welcome opens register; invalid registration makes no request', (
    tester,
  ) async {
    var calls = 0;
    final api = ApiClient(
      client: MockClient((r) async {
        calls++;
        return http.Response('{}', 500);
      }),
    );
    final session = SessionProvider(api: api, store: MemoryTokenStore());
    addTearDown(() {
      session.dispose();
      api.close();
    });
    await tester.pumpWidget(MyApp(session: session));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Get started'));
    await tester.pumpAndSettle();
    expect(find.text('Create your account'), findsOneWidget);
    await tester.enterText(find.byType(TextField).at(0), 'alex@');
    await tester.enterText(find.byType(TextField).at(1), 'alex');
    await tester.enterText(find.byType(TextField).at(2), 'short');
    await tester.tap(find.text('Create account'));
    await tester.pumpAndSettle();
    expect(find.text('Enter a valid email address.'), findsOneWidget);
    expect(
      find.text('Use at least 8 characters for your password.'),
      findsWidgets,
    );
    expect(calls, 0);
  });

  testWidgets(
    'register, automatic login, seven goals, targets and Today journey',
    (tester) async {
      tester.view.physicalSize = const Size(390, 844);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      Map<String, dynamic>? submitted;
      final api = ApiClient(
        client: MockClient((r) async {
          if (r.url.path.endsWith('register')) return http.Response('', 201);
          if (r.url.path.endsWith('login')) {
            return http.Response(jsonEncode({'token': token()}), 200);
          }
          if (r.url.path == '/api/profiles') return http.Response('{}', 404);
          if (r.url.path.endsWith('personal-data')) {
            submitted = jsonDecode(r.body) as Map<String, dynamic>;
            return http.Response(jsonEncode(savedProfile), 200);
          }
          return http.Response(
            jsonEncode({
              'date': '2026-11-17',
              'meals': [],
              'nutritionSummary': {
                'consumed': {'caloriesKcal': 0},
                'targets': {
                  'caloriesKcal': 2300,
                  'proteinGrams': 201,
                  'carbohydrateGrams': 230,
                  'fatGrams': 64,
                },
                'remaining': {
                  'caloriesKcal': 2300,
                  'proteinGrams': 201,
                  'carbohydrateGrams': 230,
                  'fatGrams': 64,
                },
              },
            }),
            200,
          );
        }),
      );
      final session = SessionProvider(api: api, store: MemoryTokenStore());
      addTearDown(() {
        session.dispose();
        api.close();
      });
      await tester.pumpWidget(MyApp(session: session));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Get started'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField).at(0), 'alex@example.com');
      await tester.enterText(find.byType(TextField).at(1), 'alex');
      await tester.enterText(find.byType(TextField).at(2), 'password123');
      await tester.tap(find.text('Create account'));
      await tester.pumpAndSettle();
      expect(find.byType(OnboardingView), findsOneWidget);
      await tester.enterText(find.byType(TextField).at(0), '28');
      await tester.enterText(find.byType(TextField).at(1), '72');
      await tester.enterText(find.byType(TextField).at(2), '178');
      await tester.tap(find.text('Imperial'));
      await tester.pumpAndSettle();
      expect(find.text('Weight · lb'), findsOneWidget);
      await tester.tap(find.text('Continue'));
      await tester.pumpAndSettle();
      expect(find.text('Higher deficit cutting'), findsOneWidget);
      await tester.scrollUntilVisible(
        find.text('Higher surplus bulking'),
        220,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.tap(find.text('Higher surplus bulking'));
      await tester.pump();
      await tester.tap(find.text('Calculate my targets'));
      await tester.pumpAndSettle();
      expect(submitted!['goalType'], 'AGGRESSIVE_BULK');
      expect(submitted!['weight'], closeTo(72, .001));
      expect(submitted!['height'], closeTo(178, .001));
      expect(find.text('Your daily starting point'), findsOneWidget);
      expect(find.text('2300'), findsOneWidget);
      await tester.tap(find.text('Start my day'));
      await tester.pumpAndSettle();
      expect(find.text('Today'), findsNWidgets(2));
      expect(find.text('0 items'), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'login failure keeps identifier, clears password and displays error',
    (tester) async {
      final api = ApiClient(
        client: MockClient((r) async => http.Response('{}', 401)),
      );
      final session = SessionProvider(api: api, store: MemoryTokenStore());
      addTearDown(() {
        session.dispose();
        api.close();
      });
      await tester.pumpWidget(MyApp(session: session));
      await tester.pumpAndSettle();
      await tester.tap(find.text('I already have an account'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField).at(0), 'alex');
      await tester.enterText(find.byType(TextField).at(1), 'wrong-password');
      await tester.tap(find.text('Log in'));
      await tester.pumpAndSettle();
      expect(
        find.text('Email, username or password is incorrect'),
        findsOneWidget,
      );
      expect(
        tester.widget<TextField>(find.byType(TextField).at(0)).controller!.text,
        'alex',
      );
      expect(
        tester.widget<TextField>(find.byType(TextField).at(1)).controller!.text,
        isEmpty,
      );
      expect(find.text('Let’s reconnect'), findsNothing);
    },
  );

  testWidgets('expired stored session opens reconnect then login', (
    tester,
  ) async {
    final api = ApiClient(
      client: MockClient((r) async => http.Response('{}', 500)),
    );
    final session = SessionProvider(
      api: api,
      store: MemoryTokenStore(token(expired: true)),
    );
    addTearDown(() {
      session.dispose();
      api.close();
    });
    await tester.pumpWidget(MyApp(session: session));
    await tester.pumpAndSettle();
    expect(find.text('Your session has expired.'), findsOneWidget);
    await tester.tap(find.text('Log in again'));
    await tester.pumpAndSettle();
    expect(find.text('Good to see you'), findsOneWidget);
  });

  testWidgets(
    'account created but automatic login fails: offer login without registering again',
    (tester) async {
      var registrations = 0;
      final api = ApiClient(
        client: MockClient((r) async {
          if (r.url.path.endsWith('register')) {
            registrations++;
            return http.Response('', 201);
          }
          return http.Response('{}', 503);
        }),
      );
      final session = SessionProvider(api: api, store: MemoryTokenStore());
      addTearDown(() {
        session.dispose();
        api.close();
      });
      await tester.pumpWidget(MyApp(session: session));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Get started'));
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField).at(0), 'alex@example.com');
      await tester.enterText(find.byType(TextField).at(1), 'alex');
      await tester.enterText(find.byType(TextField).at(2), 'password123');
      await tester.tap(find.text('Create account'));
      await tester.pumpAndSettle();
      expect(registrations, 1);
      expect(find.text('Good to see you'), findsOneWidget);
      expect(
        find.text('Your account is ready. Log in to continue.'),
        findsOneWidget,
      );
    },
  );
}
