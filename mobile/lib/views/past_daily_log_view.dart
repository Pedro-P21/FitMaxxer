import 'package:material_ui/material_ui.dart';

import '../models/daily_log.dart';
import '../models/log_history.dart';
import '../models/nutrition.dart';
import '../providers/daily_log_provider.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_feedback_card.dart';
import '../widgets/flow_scaffold.dart';
import '../widgets/journal_card.dart';
import 'copy_meal_view.dart';

class PastDailyLogView extends StatelessWidget {
  const PastDailyLogView({super.key, required this.log, required this.diary});
  final DailyLog log;
  final DailyLogProvider diary;
  Future<void> _copy(BuildContext context, LoggedMeal meal) async {
    diary.clearFeedback();
    final copied = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => CopyMealView(source: log, meal: meal, diary: diary),
      ),
    );
    if (copied == true && context.mounted) Navigator.pop(context, true);
  }

  @override
  Widget build(BuildContext context) {
    final empty = mealTypes
        .where((type) => log.meal(type).items.isEmpty)
        .toList();
    return FlowScaffold(
      eyebrow: '${log.date.year} · PAST DAILY LOG',
      title: dayLabel(log.date),
      backLabel: 'History',
      onBack: () => Navigator.pop(context),
      actions: const [],
      children: [
        const AppFeedbackCard(
          title: 'Read-only day',
          detail: 'You can copy a whole meal, but can’t change past entries.',
        ),
        JournalCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '${log.consumed.calories.round()} kcal',
                style: AppTheme.textStyle(
                  30,
                  weight: FontWeight.w700,
                  color: AppColors.blue,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                'Protein ${amountText(log.consumed.protein)} g · Carbs ${amountText(log.consumed.carbs)} g · Fat ${amountText(log.consumed.fat)} g',
                style: AppTheme.textStyle(12, color: AppColors.muted),
              ),
            ],
          ),
        ),
        for (final type in mealTypes)
          if (log.meal(type).items.isNotEmpty)
            JournalCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          type,
                          style: AppTheme.textStyle(
                            17,
                            weight: FontWeight.w700,
                          ),
                        ),
                      ),
                      Text(
                        '${log.meal(type).consumed.calories.round()} kcal',
                        style: AppTheme.textStyle(
                          14,
                          weight: FontWeight.w600,
                          color: AppColors.blue,
                        ),
                      ),
                    ],
                  ),
                  for (final item in log.meal(type).items) ...[
                    const SizedBox(height: 8),
                    Text(item.name, style: AppTheme.textStyle(15)),
                    Text(
                      '${item.portion} · ${_macros(item.consumed)}',
                      style: AppTheme.textStyle(12, color: AppColors.muted),
                    ),
                  ],
                  TextButton(
                    key: ValueKey('copy-source-$type'),
                    onPressed: () => _copy(context, log.meal(type)),
                    child: const Text('Copy whole meal to today →'),
                  ),
                ],
              ),
            ),
        if (empty.isNotEmpty)
          JournalCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  empty.join(' & '),
                  style: AppTheme.textStyle(15, weight: FontWeight.w600),
                ),
                const Text('No entries · empty meals can’t be copied'),
              ],
            ),
          ),
      ],
    );
  }

  String _macros(Nutrients? n) => n == null
      ? 'Nutrition unavailable'
      : 'P ${amountText(n.protein)} g · C ${amountText(n.carbs)} g · F ${amountText(n.fat)} g';
}
