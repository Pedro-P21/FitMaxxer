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
}
