import 'package:flutter/foundation.dart';

import '../models/daily_log.dart';
import '../services/api_client.dart';
import '../services/daily_log_service.dart';

class LogHistoryProvider extends ChangeNotifier {
  LogHistoryProvider(this.service, {DateTime Function()? today})
    : _today = today ?? DateTime.now {
    final now = _today();
    month = DateTime(now.year, now.month);
  }
  final DailyLogService service;
  final DateTime Function() _today;
  late DateTime month;
  List<DailyLog> logs = [];
  bool loading = false, loaded = false, _disposed = false;
  String? error;
  int _request = 0;
  DateTime get latestMonth => DateTime(_today().year, _today().month);
  DateTime get earliestMonth =>
      DateTime(latestMonth.year - 10, latestMonth.month);

  bool supports(DateTime date) {
    final candidate = DateTime(date.year, date.month);
    return !candidate.isAfter(latestMonth) &&
        !candidate.isBefore(earliestMonth);
  }

  Future<void> load([DateTime? selected]) async {
    final candidate = selected == null
        ? month
        : DateTime(selected.year, selected.month);
    if (!supports(candidate) || _disposed) return;
    if (candidate != month) {
      logs = [];
      loaded = false;
    }
    month = candidate;
    final request = ++_request;
    loading = true;
    error = null;
    notifyListeners();
    try {
      final result = await service.history(month.year, month.month);
      if (_disposed || request != _request) return;
      final today = _today();
      final cutoff = DateTime(today.year, today.month, today.day);
      logs =
          result.logs
              .where(
                (log) =>
                    log.date.isBefore(cutoff) &&
                    log.date.year == month.year &&
                    log.date.month == month.month,
              )
              .toList()
            ..sort((a, b) => b.date.compareTo(a.date));
      loaded = true;
    } catch (e) {
      if (!_disposed && request == _request) {
        error = e is ApiException
            ? e.message
            : 'Your history could not be loaded. Please try again.';
      }
    } finally {
      if (!_disposed && request == _request) {
        loading = false;
        notifyListeners();
      }
    }
  }

  @override
  void dispose() {
    _disposed = true;
    _request++;
    super.dispose();
  }
}
