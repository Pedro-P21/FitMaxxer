import 'package:material_ui/material_ui.dart';

import '../models/nutrition.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';

class NutritionCard extends StatelessWidget {
  const NutritionCard({
    super.key,
    required this.nutrients,
    this.label = 'NUTRITION FOR THIS PORTION',
  });
  final Nutrients? nutrients;
  final String label;
  @override
  Widget build(BuildContext context) => Container(
    width: double.infinity,
    padding: const EdgeInsets.all(20),
    decoration: BoxDecoration(
      color: AppColors.white,
      borderRadius: BorderRadius.circular(24),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: AppTheme.textStyle(
            10,
            weight: FontWeight.w500,
            color: AppColors.muted,
          ),
        ),
        const SizedBox(height: 14),
        Text(
          '${nutrients?.calories.round() ?? '—'} kcal',
          style: AppTheme.textStyle(
            36,
            weight: FontWeight.w700,
            color: AppColors.blue,
            height: 1.2,
          ),
        ),
        const SizedBox(height: 14),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (final e in {
              'Protein': nutrients?.protein,
              'Carbs': nutrients?.carbs,
              'Fat': nutrients?.fat,
            }.entries)
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${e.value?.round() ?? '—'} g',
                      style: AppTheme.textStyle(18, weight: FontWeight.w700),
                    ),
                    Text(
                      e.key,
                      style: AppTheme.textStyle(11, color: AppColors.muted),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ],
    ),
  );
}

class InlineNotice extends StatelessWidget {
  const InlineNotice(
    this.message, {
    super.key,
    this.error = false,
    this.success = false,
    this.warning = false,
  });
  final String message;
  final bool error, success, warning;
  @override
  Widget build(BuildContext context) => Semantics(
    liveRegion: true,
    child: Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: error
            ? AppColors.pale
            : warning
            ? AppColors.warningBackground
            : success
            ? AppColors.successBackground
            : AppColors.pale,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        children: [
          if (!error) ...[
            Icon(
              warning ? Icons.warning_amber_rounded : Icons.check,
              size: 16,
              color: warning
                  ? AppColors.warning
                  : success
                  ? AppColors.success
                  : AppColors.blue,
            ),
            const SizedBox(width: 6),
          ],
          Expanded(
            child: Text(
              message,
              style: AppTheme.textStyle(
                13,
                weight: FontWeight.w500,
                color: error
                    ? AppColors.destructive
                    : warning
                    ? AppColors.warning
                    : success
                    ? AppColors.success
                    : AppColors.blue,
              ),
            ),
          ),
        ],
      ),
    ),
  );
}
