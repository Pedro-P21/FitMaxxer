import 'package:material_ui/material_ui.dart';

import '../models/daily_log.dart';
import '../models/log_history.dart';
import '../providers/daily_log_provider.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_button.dart';
import '../widgets/flow_scaffold.dart';
import '../widgets/journal_card.dart';
import '../widgets/nutrition_card.dart';

class CopyMealView extends StatefulWidget {
  const CopyMealView({
    super.key,
    required this.source,
    required this.meal,
    required this.diary,
  });
  final DailyLog source;
  final LoggedMeal meal;
  final DailyLogProvider diary;
  @override
  State<CopyMealView> createState() => _CopyMealViewState();
}

class _CopyMealViewState extends State<CopyMealView> {
  late String _destination;
  @override
  void initState() {
    super.initState();
    _destination = widget.meal.type;
  }

  Future<void> _copy() async {
    if (widget.meal.items.isEmpty || widget.diary.busy) return;
    final success = await widget.diary.copyMeal(
      widget.source.date,
      widget.meal.type,
      _destination,
    );
    if (success && mounted) Navigator.pop(context, true);
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: widget.diary,
    builder: (context, _) => PopScope(
      canPop: !widget.diary.busy,
      child: FlowScaffold(
        eyebrow: 'COPY TO TODAY',
        title: 'Bring a meal forward',
        backLabel: 'Past daily log',
        onBack: widget.diary.busy ? null : () => Navigator.pop(context),
        actions: [
          AppButton(
            label: 'Copy meal to $_destination',
            isLoading: widget.diary.busy,
            onPressed: widget.meal.items.isEmpty ? null : _copy,
          ),
        ],
        children: [
          JournalCard(
            outlined: true,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '${widget.meal.type} · ${dayLabel(widget.source.date)}',
                  style: AppTheme.textStyle(16, weight: FontWeight.w600),
                ),
                const SizedBox(height: 6),
                Text(
                  '${widget.meal.items.length} ${widget.meal.items.length == 1 ? 'item' : 'items'} · ${widget.meal.consumed.calories.round()} kcal',
                ),
                for (final item in widget.meal.items)
                  Text(
                    '${item.name} · ${item.portion}',
                    style: AppTheme.textStyle(12, color: AppColors.muted),
                  ),
              ],
            ),
          ),
          Text(
            'Choose a meal for today',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          for (final type in mealTypes)
            Semantics(
              selected: _destination == type,
              button: true,
              child: JournalCard(
                key: ValueKey('copy-destination-$type'),
                selected: _destination == type,
                onTap: widget.diary.busy
                    ? null
                    : () => setState(() => _destination = type),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            type,
                            style: AppTheme.textStyle(
                              16,
                              weight: FontWeight.w600,
                            ),
                          ),
                          Text(
                            'Append copied items',
                            style: AppTheme.textStyle(
                              12,
                              color: AppColors.muted,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Icon(
                      _destination == type
                          ? Icons.radio_button_checked
                          : Icons.radio_button_off,
                      color: AppColors.blue,
                      size: 20,
                    ),
                  ],
                ),
              ),
            ),
          const Text(
            'All items, quantities and nutrition are preserved. Copying adds entries; it never replaces a meal. You can copy again.',
          ),
          if (widget.diary.error != null)
            InlineNotice(widget.diary.error!, error: true),
        ],
      ),
    ),
  );
}
