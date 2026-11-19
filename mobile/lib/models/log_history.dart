import 'daily_log.dart';

class LogHistory {
  LogHistory.fromJson(Map<String, dynamic> json)
    : year = (json['year'] as num).toInt(),
      month = (json['month'] as num).toInt(),
      logs = (json['logs'] as List)
          .map((log) => DailyLog.fromJson(log as Map<String, dynamic>))
          .toList();
  final int year, month;
  final List<DailyLog> logs;
}

const monthNames = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];
const weekdayNames = [
  'Monday',
  'Tuesday',
  'Wednesday',
  'Thursday',
  'Friday',
  'Saturday',
  'Sunday',
];
String monthLabel(DateTime month) =>
    '${monthNames[month.month - 1]} ${month.year}';
String dayLabel(DateTime day) =>
    '${weekdayNames[day.weekday - 1]}, ${monthNames[day.month - 1]} ${day.day}';
String apiDate(DateTime day) =>
    '${day.year.toString().padLeft(4, '0')}-${day.month.toString().padLeft(2, '0')}-${day.day.toString().padLeft(2, '0')}';
