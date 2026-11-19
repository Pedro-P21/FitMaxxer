import 'package:material_ui/material_ui.dart';

import 'app_colors.dart';

abstract final class AppTheme {
  static const spacingUnit = 8.0;
  static const componentRadius = 18.0;
  static const screenPadding = 20.0;

  static const primaryGradient = LinearGradient(
    begin: Alignment(-0.066, -2.245),
    end: Alignment(0.066, 2.245),
    colors: [
      AppColors.gradientStart,
      AppColors.gradientMiddle,
      AppColors.gradientEnd,
    ],
    stops: [0, 0.41429, 0.71429],
  );

  static const targetGradient = LinearGradient(
    begin: Alignment(-0.526, -1.50),
    end: Alignment(0.526, 1.50),
    colors: [
      AppColors.gradientStart,
      AppColors.gradientMiddle,
      AppColors.gradientEnd,
    ],
    stops: [0, .41429, .71429],
  );
  static const todayGradient = LinearGradient(
    begin: Alignment(-0.745, -1.34),
    end: Alignment(0.745, 1.34),
    colors: [
      AppColors.gradientStart,
      AppColors.gradientMiddle,
      AppColors.gradientEnd,
    ],
    stops: [0, .41429, .71429],
  );
  static const sessionGradient = LinearGradient(
    begin: Alignment(-0.90, -1.12),
    end: Alignment(0.90, 1.12),
    colors: [
      AppColors.gradientStart,
      AppColors.gradientMiddle,
      AppColors.gradientEnd,
    ],
    stops: [0, .41429, .71429],
  );

  static TextStyle textStyle(
    double size, {
    FontWeight weight = FontWeight.w400,
    Color color = AppColors.ink,
    double height = 1.4,
  }) => TextStyle(
    fontFamily: 'Inter',
    fontSize: size,
    fontWeight: weight,
    color: color,
    height: height,
  );

  static ThemeData get light => ThemeData(
    useMaterial3: true,
    fontFamily: 'Inter',
    scaffoldBackgroundColor: AppColors.background,
    colorScheme: const ColorScheme.light(
      primary: AppColors.blue,
      onPrimary: AppColors.white,
      secondary: AppColors.pale,
      onSecondary: AppColors.blue,
      surface: AppColors.white,
      onSurface: AppColors.ink,
      onSurfaceVariant: AppColors.muted,
      outline: AppColors.line,
      error: AppColors.destructive,
      onError: AppColors.white,
    ),
    textTheme: TextTheme(
      headlineLarge: textStyle(32, weight: FontWeight.w700, height: 1.35),
      headlineMedium: textStyle(28, weight: FontWeight.w700, height: 1.35),
      headlineSmall: textStyle(24, weight: FontWeight.w700),
      titleMedium: textStyle(15, weight: FontWeight.w600),
      bodyLarge: textStyle(16, color: AppColors.muted, height: 1.35),
      bodyMedium: textStyle(15, color: AppColors.muted, height: 1.35),
      bodySmall: textStyle(12, color: AppColors.muted, height: 1.35),
      labelLarge: textStyle(16, weight: FontWeight.w600),
      labelMedium: textStyle(13, weight: FontWeight.w600),
      labelSmall: textStyle(11, weight: FontWeight.w500),
    ),
    textSelectionTheme: const TextSelectionThemeData(
      cursorColor: AppColors.blue,
      selectionColor: AppColors.pale,
      selectionHandleColor: AppColors.blue,
    ),
  );
}
