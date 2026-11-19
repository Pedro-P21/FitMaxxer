import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/providers/daily_log_provider.dart';
import 'package:mobile/providers/food_catalog_provider.dart';
import 'package:mobile/services/api_client.dart';
import 'package:mobile/services/daily_log_service.dart';
import 'package:mobile/services/food_catalog_service.dart';

import 'logging_fixtures.dart';

void main() {
  test(
    'out-of-order catalog responses cannot replace the latest search',
    () async {
      final first = Completer<http.Response>(),
          second = Completer<http.Response>();
      final api = ApiClient(
        client: MockClient(
          (r) => r.url.queryParameters['query'] == 'old'
              ? first.future
              : second.future,
        ),
      );
      api.token = 'test';
      final catalog = FoodCatalogProvider(FoodCatalogService(api));
      addTearDown(() {
        catalog.dispose();
        api.close();
      });
      final oldSearch = catalog.search('old');
      final newSearch = catalog.search('Greek & yogurt');
      second.complete(
        http.Response(
          jsonEncode({
            'items': [yogurtFood],
          }),
          200,
        ),
      );
      await newSearch;
      first.complete(http.Response('{"items":[]}', 200));
      await oldSearch;
      expect(catalog.foods.single.name, 'Greek yogurt');
      expect(catalog.query, 'Greek & yogurt');
    },
  );
  test('failed writes retain data; success uses returned log and exact API payloads', () async {
    var fail = true;
    final requests = <http.Request>[];
    final api = ApiClient(
      client: MockClient((r) async {
        requests.add(r);
        if (r.method == 'GET') {
          return http.Response(jsonEncode(diaryJson(quantity: 150)), 200);
        }
        if (fail) return http.Response('{}', 409);
        return http.Response(
          jsonEncode(diaryJson(quantity: 200, meal: 'Dinner')),
          200,
        );
      }),
    )..token = 'test';
    final diary = DailyLogProvider(DailyLogService(api));
    addTearDown(() {
      diary.dispose();
      api.close();
    });
    await diary.load();
    final original = diary.log!;
    expect(await diary.quantity(original.item(1)!, 200), isFalse);
    expect(diary.log, same(original));
    expect(diary.error, isNotNull);
    fail = false;
    expect(await diary.quantity(original.item(1)!, 200), isTrue);
    expect(jsonDecode(requests.last.body), {'quantity': 200});
    expect(requests.last.url.path, '/api/daily-log/today/items/1/quantity');
    expect(diary.log!.consumed.calories, 200);
    expect(diary.message, contains('200 g'));
    await diary.move(diary.log!.item(1)!, 'Lunch');
    expect(jsonDecode(requests.last.body), {'destinationMealType': 'Lunch'});
    expect(diary.message, contains('Moved to Lunch'));
  });
  test('late GET does not overwrite a mutation and duplicate mutations are blocked', () async {
    final read = Completer<http.Response>(), write = Completer<http.Response>();
    var writes = 0;
    final api = ApiClient(
      client: MockClient((r) {
        if (r.method == 'GET') return read.future;
        writes++;
        return write.future;
      }),
    )..token = 'test';
    final diary = DailyLogProvider(DailyLogService(api));
    addTearDown(() {
      diary.dispose();
      api.close();
    });
    final loading = diary.load();
    final adding = diary.add('food', 'Snacks', 150, 'GRAM');
    expect(await diary.add('food', 'Snacks', 150, 'GRAM'), isFalse);
    write.complete(http.Response(jsonEncode(diaryJson(quantity: 150)), 200));
    await adding;
    read.complete(http.Response(jsonEncode(diaryJson()), 200));
    await loading;
    expect(writes, 1);
    expect(diary.log!.consumed.calories, 150);
  });
  test(
    'catalog distinguishes empty results from connection/server failure',
    () async {
      var status = 200;
      final api = ApiClient(
        client: MockClient((r) async => http.Response('{"items":[]}', status)),
      )..token = 'test';
      final catalog = FoodCatalogProvider(FoodCatalogService(api));
      addTearDown(() {
        catalog.dispose();
        api.close();
      });
      await catalog.search('unknown');
      expect(catalog.searched, isTrue);
      expect(catalog.error, isNull);
      expect(catalog.foods, isEmpty);
      status = 503;
      await catalog.search('unknown');
      expect(catalog.error, isNotNull);
      expect(catalog.searched, isFalse);
    },
  );
}
