import 'package:material_ui/material_ui.dart';

import '../theme/app_colors.dart';
import '../theme/app_theme.dart';

enum AppButtonVariant { primary, secondary, destructive }

/// Full-width Figma button. A null callback or loading state prevents taps.
class AppButton extends StatelessWidget {
  const AppButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.variant = AppButtonVariant.primary,
    this.isLoading = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final AppButtonVariant variant;
  final bool isLoading;

  @override
  Widget build(BuildContext context) {
    final primary = variant == AppButtonVariant.primary;
    final foreground = switch (variant) {
      AppButtonVariant.primary => AppColors.white,
      AppButtonVariant.secondary => AppColors.blue,
      AppButtonVariant.destructive => AppColors.destructive,
    };
    final background = switch (variant) {
      AppButtonVariant.primary => null,
      AppButtonVariant.secondary => AppColors.pale,
      AppButtonVariant.destructive => AppColors.destructiveBackground,
    };
    final enabled = onPressed != null && !isLoading;
    final radius = BorderRadius.circular(AppTheme.componentRadius);

    return Opacity(
      opacity: enabled || isLoading ? 1 : 0.5,
      child: Material(
        color: Colors.transparent,
        borderRadius: radius,
        clipBehavior: Clip.antiAlias,
        child: Ink(
          decoration: BoxDecoration(
            color: background,
            gradient: primary ? AppTheme.primaryGradient : null,
            borderRadius: radius,
          ),
          child: TextButton(
            onPressed: enabled ? onPressed : null,
            style: TextButton.styleFrom(
              foregroundColor: foreground,
              disabledForegroundColor: foreground,
              minimumSize: Size(double.infinity, primary ? 52 : 48),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              shape: RoundedRectangleBorder(borderRadius: radius),
              textStyle: AppTheme.textStyle(
                primary ? 16 : 15,
                weight: FontWeight.w600,
                color: foreground,
              ),
            ),
            child: isLoading
                ? Semantics(
                    label: '$label, loading',
                    liveRegion: true,
                    child: SizedBox.square(
                      dimension: 22,
                      child: CircularProgressIndicator(
                        color: foreground,
                        strokeWidth: 2,
                      ),
                    ),
                  )
                : Text(label, textAlign: TextAlign.center),
          ),
        ),
      ),
    );
  }
}
