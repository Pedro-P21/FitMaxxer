import 'package:material_ui/material_ui.dart';

import '../theme/app_colors.dart';
import '../theme/app_theme.dart';

/// A scrollable Figma screen body with actions anchored above the safe area.
class FlowScaffold extends StatelessWidget {
  const FlowScaffold({
    super.key,
    required this.eyebrow,
    required this.title,
    required this.children,
    required this.actions,
    this.onBack,
    this.bodySpacing = 14,
    this.backLabel = 'Back',
  });
  final String eyebrow, title;
  final String backLabel;
  final List<Widget> children, actions;
  final VoidCallback? onBack;
  final double bodySpacing;
  @override
  Widget build(BuildContext context) => Scaffold(
    body: SafeArea(
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 10, 20, 8),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (onBack != null)
                  TextButton(
                    onPressed: onBack,
                    style: TextButton.styleFrom(
                      padding: EdgeInsets.zero,
                      minimumSize: const Size(48, 18),
                      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                      textStyle: AppTheme.textStyle(
                        13,
                        weight: FontWeight.w600,
                      ),
                      alignment: Alignment.centerLeft,
                    ),
                    child: Text('‹  $backLabel'),
                  ),
                Text(
                  eyebrow,
                  style: AppTheme.textStyle(
                    11,
                    weight: FontWeight.w600,
                    color: AppColors.blue,
                    height: 1.35,
                  ),
                ),
                const SizedBox(height: 2),
                SizedBox(
                  width: double.infinity,
                  child: Text(
                    title,
                    style: Theme.of(context).textTheme.headlineMedium,
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: SingleChildScrollView(
              keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
              padding: const EdgeInsets.fromLTRB(20, 10, 20, 16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  for (var i = 0; i < children.length; i++) ...[
                    if (i > 0) SizedBox(height: bodySpacing),
                    children[i],
                  ],
                ],
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                for (var i = 0; i < actions.length; i++) ...[
                  if (i > 0) const SizedBox(height: 8),
                  actions[i],
                ],
              ],
            ),
          ),
        ],
      ),
    ),
  );
}
