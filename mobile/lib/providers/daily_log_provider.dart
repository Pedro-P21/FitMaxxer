import 'dart:async';

import 'package:flutter/foundation.dart';

import '../models/daily_log.dart';
import '../services/api_client.dart';
import '../services/daily_log_service.dart';

class DailyLogProvider extends ChangeNotifier {
  DailyLogProvider(this.service);
  final DailyLogService service;
  DailyLog? log;
  bool loading = false, busy = false, _disposed = false;
  String? error, message;
  int _revision = 0, _loadNumber = 0;
  Timer? _confirmation;

  @override
  void notifyListeners() {
    if (!_disposed) super.notifyListeners();
  }

  Future<void> load() async {
    if (busy) return;
    final revision = _revision, number = ++_loadNumber;
    loading = true;
    error = null;
    notifyListeners();
    try {
      final result = await service.today();
      if (revision == _revision && number == _loadNumber) log = result;
    } catch (e) {
      if (revision == _revision && number == _loadNumber) error = _error(e);
    } finally {
      if (number == _loadNumber) {
        loading = false;
        notifyListeners();
      }
    }
  }

  Future<bool> _mutate(
    Future<DailyLog> Function() action,
    String confirmation,
  ) async {
    if (busy || _disposed) return false;
    busy = true;
    error = null;
    message = null;
    _revision++;
    notifyListeners();
    try {
      final result = await action();
      if (_disposed) return false;
      log = result;
      message = confirmation;
      _confirmation?.cancel();
      _confirmation = Timer(const Duration(seconds: 5), () {
        message = null;
        notifyListeners();
      });
      return true;
    } catch (e) {
      error = _error(e);
      return false;
    } finally {
      busy = false;
      notifyListeners();
    }
  }

  Future<bool> add(String foodId, String meal, double quantity, String unit) =>
      _mutate(
        () => service.add(foodId, meal, quantity, unit),
        'Added to $meal · totals updated',
      );
  void clearFeedback() {
    _confirmation?.cancel();
    message = null;
    error = null;
    notifyListeners();
  }

  String _error(Object e) => e is ApiException
      ? e.message
      : 'Unable to update your diary. Please try again.';
  @override
  void dispose() {
    _confirmation?.cancel();
    _disposed = true;
    super.dispose();
  }
}
