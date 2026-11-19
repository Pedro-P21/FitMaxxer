import 'dart:math' as math;

import 'package:material_ui/material_ui.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../theme/app_colors.dart';
import '../theme/app_theme.dart';

/// The original welcome artwork, shared with launcher icon generation.
class AppLogo extends StatelessWidget {
  const AppLogo({super.key, this.showWordmark = true});
  final bool showWordmark;

  @override
  Widget build(BuildContext context) => SizedBox(
    height: showWordmark ? 272 : 200,
    width: double.infinity,
    child: Center(
      child: SizedBox(
        width: 240,
        height: showWordmark ? 272 : 200,
        child: Stack(
          children: [
            Positioned(
              left: 0,
              top: 7,
              width: 240,
              height: 200,
              child: Stack(
                children: [
                  Positioned(
                    left: 3.48,
                    top: 9.5,
                    width: 232,
                    height: 64.5,
                    child: Transform.rotate(
                      angle: math.pi,
                      child: Transform.flip(
                        flipX: true,
                        child: SvgPicture.asset(
                          'assets/images/banana_barbell.svg',
                        ),
                      ),
                    ),
                  ),
                  Positioned(
                    left: 69.74,
                    top: 34,
                    width: 100.52,
                    height: 157.2,
                    child: SvgPicture.asset('assets/images/weightlifter.svg'),
                  ),
                ],
              ),
            ),
            if (showWordmark)
              Positioned(
                top: 220,
                left: 0,
                right: 0,
                child: Text(
                  'Fitmaxxer',
                  textAlign: TextAlign.center,
                  style: AppTheme.textStyle(
                    30,
                    weight: FontWeight.w700,
                    color: AppColors.gradientStart,
                    height: 1.2,
                  ).copyWith(letterSpacing: -0.9),
                ),
              ),
          ],
        ),
      ),
    ),
  );
}
