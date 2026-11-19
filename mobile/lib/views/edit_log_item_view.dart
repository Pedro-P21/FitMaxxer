import 'package:material_ui/material_ui.dart';

import '../models/daily_log.dart';
import '../models/nutrition.dart';
import '../providers/daily_log_provider.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_button.dart';
import '../widgets/app_feedback_card.dart';
import '../widgets/app_text_field.dart';
import '../widgets/flow_scaffold.dart';
import '../widgets/nutrition_card.dart';
import 'move_log_item_view.dart';
import 'delete_log_item_view.dart';

class EditLogItemView extends StatefulWidget {
  const EditLogItemView({super.key, required this.item, required this.diary});
  final LoggedItem item;
  final DailyLogProvider diary;
  @override
  State<EditLogItemView> createState() => _EditLogItemViewState();
}

class _EditLogItemViewState extends State<EditLogItemView> {
  late final TextEditingController _quantity;
  final _form = GlobalKey<FormState>();
  LoggedItem get item => widget.diary.log?.item(widget.item.id) ?? widget.item;
  double get entered =>
      double.tryParse(_quantity.text.trim().replaceAll(',', '.')) ?? 0;
  bool get dirty => entered != item.quantity;
  @override
  void initState() {
    super.initState();
    _quantity = TextEditingController(text: amountText(item.quantity));
  }

  @override
  void dispose() {
    _quantity.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    FocusScope.of(context).unfocus();
    if (!_form.currentState!.validate()) return;
    final success = await widget.diary.quantity(item, entered);
    if (success && mounted) Navigator.pop(context);
  }

  Future<void> _action(Widget screen) async {
    FocusScope.of(context).unfocus();
    final success = await Navigator.of(context)
        .push<bool>(MaterialPageRoute(builder: (_) => screen));
    if (success == true && mounted) Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: widget.diary,
    builder: (context, _) {
      final current = item;
      final diary = widget.diary;
      final exists = diary.log?.item(current.id) != null;
      final preview = quantityError(_quantity.text) == null
          ? current.nutrition.forQuantity(entered, current.unit)
          : null;
      final total =
          preview != null && current.consumed != null && diary.log != null
          ? diary.log!.consumed - current.consumed! + preview
          : null;
      final remaining = total == null
          ? null
          : diary.log!.targets.calories - total.calories;
      return PopScope(
        canPop: !diary.busy,
        child: Form(
          key: _form,
          child: FlowScaffold(
            eyebrow:
                'TODAY · ${diary.log?.mealFor(current.id)?.toUpperCase() ?? 'ITEM'}',
            title: 'Edit food',
            backLabel: 'Today',
            onBack: diary.busy ? null : () => Navigator.pop(context),
            actions: [
              AppButton(
                label: 'Save quantity',
                isLoading: diary.busy,
                onPressed:
                    !exists || !dirty || quantityError(_quantity.text) != null
                    ? null
                    : _save,
              ),
            ],
            children: [
              Text(
                current.name,
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: AppTextField(
                      label: 'Quantity',
                      controller: _quantity,
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                      enabled: !diary.busy,
                      validator: quantityError,
                      autovalidateMode: AutovalidateMode.onUserInteraction,
                      onChanged: (_) => setState(() {}),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 12,
                      ),
                      constraints: const BoxConstraints(minHeight: 74),
                      decoration: BoxDecoration(
                        color: AppColors.white,
                        border: Border.all(color: AppColors.line),
                        borderRadius: BorderRadius.circular(18),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Unit',
                            style: AppTheme.textStyle(
                              11,
                              color: AppColors.muted,
                            ),
                          ),
                          Text(
                            unitLabel(current.unit),
                            style: AppTheme.textStyle(
                              16,
                              weight: FontWeight.w500,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
              NutritionCard(nutrients: preview),
              if (total != null)
                AppFeedbackCard(
                  title: 'Daily total after saving',
                  detail:
                      '${total.calories.round()} / ${diary.log!.targets.calories.round()} kcal · '
                      '${remaining!.abs().round()} kcal ${remaining < 0 ? 'over target' : 'remaining'}',
                ),
              if (dirty)
                Text(
                  'Save the quantity before moving this item.',
                  style: AppTheme.textStyle(12, color: AppColors.muted),
                ),
              AppButton(
                label: 'Move to another meal',
                variant: AppButtonVariant.secondary,
                onPressed: diary.busy || dirty || !exists
                    ? null
                    : () =>
                          _action(MoveLogItemView(item: current, diary: diary)),
              ),
              AppButton(
                label: 'Delete this item',
                variant: AppButtonVariant.destructive,
                onPressed: diary.busy || !exists
                    ? null
                    : () => _action(
                        DeleteLogItemView(item: current, diary: diary),
                      ),
              ),
              if (!exists)
                const InlineNotice(
                  'This item is no longer in today’s diary. Return to Today to refresh.',
                  error: true,
                ),
              if (diary.error != null) InlineNotice(diary.error!, error: true),
            ],
          ),
        ),
      );
    },
  );
}
