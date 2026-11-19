import 'package:material_ui/material_ui.dart';

import '../models/daily_log.dart';
import '../models/log_history.dart';
import '../providers/daily_log_provider.dart';
import '../providers/log_history_provider.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_bottom_navigation.dart';
import '../widgets/app_button.dart';
import '../widgets/app_feedback_card.dart';
import '../widgets/journal_card.dart';
import 'history_month_view.dart';
import 'past_daily_log_view.dart';

class HistoryView extends StatefulWidget {
  const HistoryView({
    super.key,
    required this.history,
    required this.diary,
    required this.onSelectTab,
    required this.onCopied,
  });
  final LogHistoryProvider history;
  final DailyLogProvider diary;
  final ValueChanged<String> onSelectTab;
  final VoidCallback onCopied;
  @override
  State<HistoryView> createState() => _HistoryViewState();
}

class _HistoryViewState extends State<HistoryView> {
  @override
  void initState() {
    super.initState();
    if (!widget.history.loaded && !widget.history.loading) {
      widget.history.load();
    }
  }

  Future<void> _chooseMonth() async {
    final history = widget.history;
    final selected = await Navigator.of(context).push<DateTime>(
      MaterialPageRoute(
        builder: (_) => HistoryMonthView(
          selected: history.month,
          earliest: history.earliestMonth,
          latest: history.latestMonth,
        ),
      ),
    );
    if (mounted && selected != null) await history.load(selected);
  }

  Future<void> _open(DailyLog log) async {
    final copied = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => PastDailyLogView(log: log, diary: widget.diary),
      ),
    );
    if (copied == true && mounted) widget.onCopied();
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: widget.history,
    builder: (context, _) {
      final history = widget.history;
      final empty =
          history.loaded &&
          !history.loading &&
          history.error == null &&
          history.logs.isEmpty;
      return Scaffold(
        bottomNavigationBar: AppBottomNavigation(
          selected: 'History',
          onSelect: widget.onSelectTab,
        ),
        body: SafeArea(
          child: RefreshIndicator(
            onRefresh: history.load,
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(20),
              children: [
                if (empty)
                  TextButton(
                    style: TextButton.styleFrom(
                      alignment: Alignment.centerLeft,
                      padding: EdgeInsets.zero,
                    ),
                    onPressed: () => history.load(history.latestMonth),
                    child: const Text('‹ History'),
                  ),
                Text(
                  empty
                      ? monthLabel(history.month).toUpperCase()
                      : 'LOOK BACK, KEEP MOVING',
                  style: AppTheme.textStyle(
                    10,
                    color: AppColors.blue,
                    weight: FontWeight.w600,
                  ),
                ),
                Text(
                  empty ? 'No logs here yet' : 'Your food journal',
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: 22),
                AppButton(
                  label: '${monthLabel(history.month)} ⌄',
                  variant: AppButtonVariant.secondary,
                  onPressed: _chooseMonth,
                ),
                const SizedBox(height: 18),
                if (history.loading) ...[
                  const Center(
                    child: Padding(
                      padding: EdgeInsets.all(28),
                      child: CircularProgressIndicator(),
                    ),
                  ),
                  const Center(child: Text('Loading your food journal…')),
                ] else if (history.error != null) ...[
                  AppFeedbackCard(
                    title: 'Your journal couldn’t load',
                    detail: history.error!,
                  ),
                  const SizedBox(height: 14),
                  AppButton(label: 'Try again', onPressed: history.load),
                  const SizedBox(height: 10),
                  AppButton(
                    label: 'Go to Today',
                    variant: AppButtonVariant.secondary,
                    onPressed: () => widget.onSelectTab('Today'),
                  ),
                ] else if (empty) ...[
                  Text(
                    'A fresh page in your journal.',
                    style: Theme.of(context).textTheme.headlineLarge,
                  ),
                  const SizedBox(height: 14),
                  const Text(
                    'There are no saved daily logs for this month. Choose another month or start logging today.',
                  ),
                  const SizedBox(height: 14),
                  AppButton(
                    label: 'Choose another date',
                    variant: AppButtonVariant.secondary,
                    onPressed: _chooseMonth,
                  ),
                  const SizedBox(height: 14),
                  AppButton(
                    label: 'Go to Today',
                    onPressed: () => widget.onSelectTab('Today'),
                  ),
                ] else ...[
                  Text(
                    'Daily logs',
                    style: AppTheme.textStyle(17, weight: FontWeight.w700),
                  ),
                  const SizedBox(height: 14),
                  for (final log in history.logs) ...[
                    JournalCard(
                      key: ValueKey('history-day-${apiDate(log.date)}'),
                      onTap: () => _open(log),
                      child: Row(
                        children: [
                          Container(
                            width: 60,
                            padding: const EdgeInsets.symmetric(vertical: 8),
                            decoration: BoxDecoration(
                              color: AppColors.pale,
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Column(
                              children: [
                                Text(
                                  '${log.date.day}',
                                  style: AppTheme.textStyle(
                                    26,
                                    weight: FontWeight.w700,
                                    color: AppColors.blue,
                                  ),
                                ),
                                Text(
                                  monthNames[log.date.month - 1]
                                      .substring(0, 3)
                                      .toUpperCase(),
                                  style: AppTheme.textStyle(
                                    11,
                                    color: AppColors.blue,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  weekdayNames[log.date.weekday - 1]
                                      .toUpperCase(),
                                  style: AppTheme.textStyle(
                                    11,
                                    weight: FontWeight.w600,
                                    color: AppColors.muted,
                                  ),
                                ),
                                Text(
                                  '${log.consumed.calories.round()} kcal',
                                  style: AppTheme.textStyle(
                                    21,
                                    weight: FontWeight.w700,
                                  ),
                                ),
                                Text.rich(
                                  TextSpan(
                                    children: [
                                      TextSpan(
                                        text:
                                            'P ${log.consumed.protein.round()} g',
                                        style: const TextStyle(
                                          color: AppColors.macroProtein,
                                        ),
                                      ),
                                      const TextSpan(text: '  ·  '),
                                      TextSpan(
                                        text:
                                            'C ${log.consumed.carbs.round()} g',
                                        style: const TextStyle(
                                          color: AppColors.macroCarbs,
                                        ),
                                      ),
                                      const TextSpan(text: '  ·  '),
                                      TextSpan(
                                        text: 'F ${log.consumed.fat.round()} g',
                                        style: const TextStyle(
                                          color: AppColors.macroFat,
                                        ),
                                      ),
                                    ],
                                  ),
                                  style: AppTheme.textStyle(
                                    11,
                                    color: AppColors.muted,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 14),
                  ],
                  const Text(
                    'Past logs are read-only. Open a day to copy a whole meal into today.',
                  ),
                ],
              ],
            ),
          ),
        ),
      );
    },
  );
}
