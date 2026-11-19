import 'package:material_ui/material_ui.dart';

import '../models/daily_log.dart';
import '../providers/daily_log_provider.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_button.dart';
import '../widgets/app_feedback_card.dart';
import '../widgets/flow_scaffold.dart';
import '../widgets/nutrition_card.dart';

class MoveLogItemView extends StatefulWidget {
  const MoveLogItemView({super.key, required this.item, required this.diary});
  final LoggedItem item;
  final DailyLogProvider diary;
  @override
  State<MoveLogItemView> createState() => _MoveLogItemViewState();
}

class _MoveLogItemViewState extends State<MoveLogItemView> {
  String? _destination;
  Future<void> _move() async {
    if (_destination == null) return;
    if (await widget.diary.move(widget.item, _destination!) && mounted) {
      Navigator.pop(context, true);
    }
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: widget.diary,
    builder: (context, _) {
      final current = widget.diary.log?.mealFor(widget.item.id);
      final busy = widget.diary.busy;
      return PopScope(
        canPop: !busy,
        child: FlowScaffold(
          eyebrow: 'TODAY ONLY',
          title: 'Move to a meal',
          backLabel: 'Edit item',
          onBack: busy ? null : () => Navigator.pop(context),
          actions: [
            AppButton(
              label: _destination == null
                  ? 'Choose a meal'
                  : 'Move to $_destination',
              isLoading: busy,
              onPressed:
                  _destination == null ||
                      current == null ||
                      _destination == current
                  ? null
                  : _move,
            ),
            AppButton(
              label: 'Cancel',
              variant: AppButtonVariant.secondary,
              onPressed: busy ? null : () => Navigator.pop(context),
            ),
          ],
          children: [
            AppFeedbackCard(
              title: '${widget.item.name} · ${widget.item.portion}',
              detail: 'Nutrition stays the same when you move an item.',
            ),
            for (final meal in mealTypes)
              Semantics(
                selected: _destination == meal,
                inMutuallyExclusiveGroup: true,
                enabled: meal != current && !busy,
                child: Material(
                  color: _destination == meal
                      ? AppColors.pale
                      : AppColors.white,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(22),
                    side: BorderSide(
                      color: _destination == meal
                          ? AppColors.blue
                          : AppColors.line,
                    ),
                  ),
                  clipBehavior: Clip.antiAlias,
                  child: InkWell(
                    onTap: meal == current || busy
                        ? null
                        : () => setState(() => _destination = meal),
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  meal,
                                  style: AppTheme.textStyle(
                                    15,
                                    weight: FontWeight.w600,
                                  ),
                                ),
                                Text(
                                  meal == current
                                      ? 'Current meal'
                                      : widget.diary.log!
                                            .meal(meal)
                                            .items
                                            .isEmpty
                                      ? 'Nothing logged yet'
                                      : '${widget.diary.log!.meal(meal).consumed.calories.round()} kcal logged',
                                  style: AppTheme.textStyle(
                                    12,
                                    color: AppColors.muted,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          Icon(
                            _destination == meal
                                ? Icons.radio_button_checked
                                : Icons.radio_button_unchecked,
                            color: AppColors.blue,
                            size: 20,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            if (widget.diary.error != null)
              InlineNotice(widget.diary.error!, error: true),
          ],
        ),
      );
    },
  );
}
