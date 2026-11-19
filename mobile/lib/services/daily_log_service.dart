import '../models/daily_log.dart';
import 'api_client.dart';

class DailyLogService {
  DailyLogService(this.api);
  final ApiClient api;
  Future<DailyLog> today() async =>
      DailyLog.fromJson(await api.request('GET', '/api/daily-log/today'));
  Future<DailyLog> add(
    String foodId,
    String meal,
    double quantity,
    String unit,
  ) async => DailyLog.fromJson(
    await api.request(
      'POST',
      '/api/daily-log/today/items',
      body: {
        'foodId': foodId,
        'mealType': meal,
        'quantity': quantity,
        'quantityUnit': unit,
      },
    ),
  );
  Future<DailyLog> changeQuantity(int id, double quantity) async =>
      DailyLog.fromJson(
        await api.request(
          'PATCH',
          '/api/daily-log/today/items/$id/quantity',
          body: {'quantity': quantity},
        ),
      );
  Future<DailyLog> move(int id, String meal) async => DailyLog.fromJson(
    await api.request(
      'PATCH',
      '/api/daily-log/today/items/$id/meal',
      body: {'destinationMealType': meal},
    ),
  );
  Future<DailyLog> delete(int id) async => DailyLog.fromJson(
    await api.request('DELETE', '/api/daily-log/today/items/$id'),
  );
}
