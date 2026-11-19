import 'package:material_ui/material_ui.dart';

import '../theme/app_colors.dart';
import '../theme/app_theme.dart';

/// The blue feedback card used for information and authentication errors.
class AppFeedbackCard extends StatelessWidget {
  const AppFeedbackCard({super.key, required this.title, required this.detail});

  final String title;
  final String detail;

  @override
  Widget build(BuildContext context) => Semantics(
    liveRegion: true,
    child: Container(
      width: double.infinity,
      constraints: const BoxConstraints(minHeight: 62),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: AppColors.pale,
        borderRadius: BorderRadius.circular(AppTheme.componentRadius),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: AppTheme.textStyle(
              13,
              weight: FontWeight.w600,
              color: AppColors.blue,
            ),
          ),
          const SizedBox(height: 3),
          Text(detail, style: AppTheme.textStyle(11)),
        ],
      ),
    ),
  );
}
