import 'package:material_ui/material_ui.dart';

import '../models/daily_log.dart';
import '../providers/daily_log_provider.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_button.dart';
import '../widgets/flow_scaffold.dart';
import '../widgets/nutrition_card.dart';

class DeleteLogItemView extends StatelessWidget {
  const DeleteLogItemView({super.key, required this.item, required this.diary});
  final LoggedItem item;
  final DailyLogProvider diary;
  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: diary,
    builder: (context, _) {
      final exists = diary.log?.item(item.id) != null;
      final preview = item.consumed == null
          ? null
          : diary.log!.consumed - item.consumed!;
      return PopScope(
        canPop: !diary.busy,
        child: FlowScaffold(
          eyebrow: 'TODAY · INDIVIDUAL ITEM',
          title: 'Delete this item?',
          backLabel: 'Edit item',
          onBack: diary.busy ? null : () => Navigator.pop(context),
          actions: [
            AppButton(
              label: 'Delete item',
              variant: AppButtonVariant.destructive,
              isLoading: diary.busy,
              onPressed: !exists
                  ? null
                  : () async {
                      if (await diary.delete(item) && context.mounted) {
                        Navigator.pop(context, true);
                      }
                    },
            ),
            AppButton(
              label: 'Keep item',
              variant: AppButtonVariant.secondary,
              onPressed: diary.busy ? null : () => Navigator.pop(context),
            ),
          ],
          children: [
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.white,
                border: Border.all(color: AppColors.line),
                borderRadius: BorderRadius.circular(18),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    item.name,
                    style: AppTheme.textStyle(15, weight: FontWeight.w600),
                  ),
                  Text(
                    '${item.portion} · ${item.consumed?.calories.round() ?? '—'} kcal · ${diary.log?.mealFor(item.id) ?? ''}',
                    style: AppTheme.textStyle(12, color: AppColors.muted),
                  ),
                ],
              ),
            ),
            const Text(
              'Only this entry will be removed. Other foods and saved recipes won’t change.',
            ),
            NutritionCard(
              nutrients: preview,
              label: 'DAILY TOTAL AFTER DELETING',
            ),
            if (diary.error != null) InlineNotice(diary.error!, error: true),
          ],
        ),
      );
    },
  );
}
