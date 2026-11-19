import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/models/goal_option.dart';
import 'package:mobile/providers/session_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/token_store.dart';

String token({bool expired = false}) =>
    'header.${base64Url.encode(utf8.encode(jsonEncode({'exp': DateTime.now().add(Duration(minutes: expired ? -1 : 15)).millisecondsSinceEpoch ~/ 1000})))}.signature';

class MemoryTokenStore implements TokenStore {
  MemoryTokenStore([this.value]);
  String? value;
  @override
  Future<String?> read() async => value;
  @override
  Future<void> write(String token) async {
    value = token;
  }

  @override
  Future<void> clear() async {
    value = null;
  }
}

const savedProfile = {
  'age': 28,
  'weightKg': 72,
  'heightCm': 178,
  'gender': 'MALE',
  'activityLevel': 'MODERATELY_ACTIVE',
  'bodyFatPercentage': null,
  'maintenanceCalories': 2600,
  'calorieTarget': 2300,
  'proteinGrams': 201,
  'carbsGrams': 230,
  'fatsGrams': 64,
};

void main() {
  SessionProvider sessionFor(
    FutureOr<http.Response> Function(http.Request) handler,
    MemoryTokenStore store,
  ) {
    final api = ApiClient(
      client: MockClient((request) async => await handler(request)),
      baseUrl: 'http://test',
    );
    final session = SessionProvider(api: api, store: store);
    addTearDown(() {
      session.dispose();
      api.close();
    });
    return session;
  }

  test('all seven UI names map to the existing API options', () {
    expect(GoalOption.values.map((g) => g.apiValue).toSet().length, 7);
    expect(GoalOption.values.map((g) => g.offset), [
      -500,
      -300,
      -200,
      0,
      200,
      300,
      500,
    ]);
    expect(
      GoalOption.values.any(
        (g) => g.label.toLowerCase().contains('aggressive'),
      ),
      isFalse,
    );
  });
  test('registration automatically logs in, persists token and routes missing profile', () async {
    final paths = <String>[];
    final store = MemoryTokenStore();
    final jwt = token();
    final session = sessionFor((request) {
      paths.add(request.url.path);
      if (request.url.path.endsWith('register')) {
        expect(jsonDecode(request.body), {
          'email': 'alex@example.com',
          'userId': 'alex',
          'password': 'password123',
        });
        return http.Response('', 201);
      }
      if (request.url.path.endsWith('login')) {
        return http.Response(jsonEncode({'token': jwt}), 200);
      }
      expect(request.headers['Authorization'], 'Bearer $jwt');
      return http.Response('{"detail":"Profile not found"}', 404);
    }, store);
    await session.restore();
    await session.authenticate(
      'alex',
      'password123',
      email: 'alex@example.com',
    );
    expect(paths, ['/api/auth/register', '/api/auth/login', '/api/profiles']);
    expect(store.value, jwt);
    expect(session.stage, SessionStage.onboarding);
  });
  test('valid saved token restores profile without logging in again', () async {
    final session = sessionFor((r) {
      expect(r.url.path, '/api/profiles');
      return http.Response(jsonEncode(savedProfile), 200);
    }, MemoryTokenStore(token()));
    await session.restore();
    expect(session.stage, SessionStage.ready);
    expect(session.profile!.calories, 2300);
  });
  test('expired saved token is cleared without making requests', () async {
    final store = MemoryTokenStore(token(expired: true));
    final session = sessionFor(
      (_) => throw StateError('Should not request'),
      store,
    );
    await session.restore();
    expect(session.stage, SessionStage.expired);
    expect(store.value, isNull);
  });
  test(
    'server rejects protected token: clears token and shows expired screen',
    () async {
      final store = MemoryTokenStore(token());
      final session = sessionFor(
        (_) => http.Response('{"detail":"Invalid token"}', 401),
        store,
      );
      await session.restore();
      expect(session.stage, SessionStage.expired);
      expect(session.api.token, isNull);
      expect(store.value, isNull);
    },
  );
  test(
    'profile connection failure offers retry instead of onboarding',
    () async {
      var fail = true;
      final session = sessionFor(
        (_) => fail
            ? http.Response('{}', 503)
            : http.Response(jsonEncode(savedProfile), 200),
        MemoryTokenStore(token()),
      );
      await session.restore();
      expect(session.stage, SessionStage.retry);
      fail = false;
      await session.retry();
      expect(session.stage, SessionStage.ready);
    },
  );
  test('wrong credentials do not become an expired session', () async {
    final session = sessionFor(
      (_) => http.Response('{}', 401),
      MemoryTokenStore(),
    );
    await session.restore();
    await session.authenticate('alex', 'wrong');
    expect(session.stage, SessionStage.signedOut);
    expect(session.error, 'Email, username or password is incorrect');
  });
  test(
    'registration success followed by login failure preserves account outcome',
    () async {
      final session = sessionFor(
        (r) => r.url.path.endsWith('register')
            ? http.Response('', 201)
            : http.Response('{}', 503),
        MemoryTokenStore(),
      );
      await session.restore();
      await session.authenticate(
        'alex',
        'password123',
        email: 'alex@example.com',
      );
      expect(session.registered, isTrue);
      expect(session.error, contains('account was created'));
    },
  );
  test(
    'duplicate submissions are suppressed while authentication is pending',
    () async {
      final completer = Completer<http.Response>();
      var calls = 0;
      final session = sessionFor((r) {
        calls++;
        return completer.future;
      }, MemoryTokenStore());
      await session.restore();
      final first = session.authenticate('alex', 'password123');
      await session.authenticate('alex', 'password123');
      await Future<void>.delayed(Duration.zero);
      expect(calls, 1);
      completer.complete(http.Response('{}', 401));
      await first;
    },
  );
  test(
    'profile creation sends chosen goal and uses server-calculated targets',
    () async {
      final session = sessionFor((r) {
        if (r.method == 'GET') return http.Response('{}', 404);
        expect(jsonDecode(r.body)['goalType'], 'AGGRESSIVE_BULK');
        expect(jsonDecode(r.body)['unitSystem'], 'METRIC');
        return http.Response(
          jsonEncode({...savedProfile, 'calorieTarget': 3100}),
          200,
        );
      }, MemoryTokenStore(token()));
      await session.restore();
      session.draft
        ..age = '28'
        ..weightKg = '72'
        ..heightCm = '178'
        ..goal = GoalOption.higherSurplus;
      expect(await session.saveProfile(), isTrue);
      expect(session.profile!.calories, 3100);
      expect(session.stage, SessionStage.onboarding);
      session.finishOnboarding();
      expect(session.stage, SessionStage.ready);
    },
  );
}
