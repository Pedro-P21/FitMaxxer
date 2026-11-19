import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/models/daily_log.dart';
import 'package:mobile/providers/daily_log_provider.dart';
import 'package:mobile/providers/log_history_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/daily_log_service.dart';

import 'logging_fixtures.dart';

Map<String, dynamic> past(String date) => {
  ...diaryJson(quantity: 150),
  'date': date,
};
void main() {
  test('history excludes today/future, orders newest first, and enforces month window', () async {
    final calls = <http.Request>[];
    final api = ApiClient(
      client: MockClient((r) async {
        calls.add(r);
        return http.Response(
          jsonEncode({
            'year': 2026,
            'month': 11,
            'logs': [
              past('2026-11-14'),
              past('2026-11-19'),
              past('2026-11-18'),
              past('2026-11-20'),
            ],
          }),
          200,
        );
      }),
    )..token = 'test';
    final history = LogHistoryProvider(
      DailyLogService(api),
      today: () => DateTime(2026, 11, 19),
    );
    addTearDown(() {
      history.dispose();
      api.close();
    });
    await history.load();
    expect(history.logs.map((l) => l.date.day), [18, 14]);
    expect(calls.single.url.queryParameters, {'year': '2026', 'month': '11'});
    expect(calls.single.headers['authorization'], 'Bearer test');
    await history.load(DateTime(2026, 12));
    await history.load(DateTime(2016, 10));
    expect(calls, hasLength(1));
    expect(history.supports(DateTime(2016, 11)), isTrue);
  });

  test(
    'late history responses cannot replace a different selected month',
    () async {
      final first = Completer<http.Response>(),
          second = Completer<http.Response>();
      final api = ApiClient(
        client: MockClient(
          (r) => r.url.queryParameters['month'] == '11'
              ? first.future
              : second.future,
        ),
      )..token = 'test';
      final history = LogHistoryProvider(
        DailyLogService(api),
        today: () => DateTime(2026, 11, 19),
      );
      addTearDown(() {
        history.dispose();
        api.close();
      });
      final old = history.load();
      final latest = history.load(DateTime(2026, 10));
      second.complete(http.Response('{"year":2026,"month":10,"logs":[]}', 200));
      await latest;
      first.complete(
        http.Response(
          jsonEncode({
            'year': 2026,
            'month': 11,
            'logs': [past('2026-11-18')],
          }),
          200,
        ),
      );
      await old;
      expect(history.month, DateTime(2026, 10));
      expect(history.logs, isEmpty);
      expect(history.loaded, isTrue);
      expect(history.loading, isFalse);
    },
  );

  test(
    'history failure is distinct from an empty loaded month and supports retry',
    () async {
      var fail = true;
      final api = ApiClient(
        client: MockClient(
          (_) async => fail
              ? http.Response('{}', 503)
              : http.Response('{"year":2026,"month":11,"logs":[]}', 200),
        ),
      )..token = 'test';
      final history = LogHistoryProvider(
        DailyLogService(api),
        today: () => DateTime(2026, 11, 19),
      );
      addTearDown(() {
        history.dispose();
        api.close();
      });
      await history.load();
      expect(history.error, isNotNull);
      expect(history.loaded, isFalse);
      fail = false;
      await history.load();
      expect(history.error, isNull);
      expect(history.loaded, isTrue);
      expect(history.logs, isEmpty);
    },
  );

  test(
    'copy request preserves source and duplicate taps make a single write',
    () async {
      final response = Completer<http.Response>();
      final calls = <http.Request>[];
      final api = ApiClient(
        client: MockClient((r) {
          calls.add(r);
          return response.future;
        }),
      )..token = 'test';
      final diary = DailyLogProvider(DailyLogService(api))
        ..log = DailyLog.fromJson(diaryJson(quantity: 50));
      addTearDown(() {
        diary.dispose();
        api.close();
      });
      final source = DailyLog.fromJson(past('2026-11-15'));
      final first = diary.copyMeal(source.date, 'Snacks', 'Dinner');
      expect(await diary.copyMeal(source.date, 'Snacks', 'Dinner'), isFalse);
      await Future<void>.delayed(Duration.zero);
      expect(calls, hasLength(1));
      expect(calls.single.url.path, '/api/daily-log/today/meal-copies');
      expect(jsonDecode(calls.single.body), {
        'sourceDate': '2026-11-15',
        'sourceMealType': 'Snacks',
        'destinationMealType': 'Dinner',
      });
      response.complete(
        http.Response(
          jsonEncode(diaryJson(quantity: 200, meal: 'Dinner', second: true)),
          200,
        ),
      );
      expect(await first, isTrue);
      expect(diary.log!.consumed.calories, 300);
      expect(diary.log!.meal('Dinner').items, hasLength(2));
      expect(source.meal('Snacks').items.single.quantity, 150);
      expect(diary.message, 'Snacks copied to Dinner');
    },
  );

  test(
    'failed meal copy keeps today unchanged and offers no false confirmation',
    () async {
      final api = ApiClient(
        client: MockClient(
          (_) async =>
              http.Response('{"detail":"Source meal no longer exists."}', 404),
        ),
      )..token = 'test';
      final original = DailyLog.fromJson(diaryJson(quantity: 50));
      final diary = DailyLogProvider(DailyLogService(api))..log = original;
      addTearDown(() {
        diary.dispose();
        api.close();
      });
      expect(
        await diary.copyMeal(DateTime(2026, 11, 15), 'Breakfast', 'Dinner'),
        isFalse,
      );
      expect(diary.log, same(original));
      expect(diary.message, isNull);
      expect(diary.error, 'Source meal no longer exists.');
    },
  );
}
