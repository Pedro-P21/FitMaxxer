import 'package:material_ui/material_ui.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_button.dart';
import '../widgets/flow_scaffold.dart';

class SessionExpiredView extends StatelessWidget {
  const SessionExpiredView({super.key, required this.onLogin});
  final VoidCallback onLogin;
  @override
  Widget build(BuildContext context) => FlowScaffold(
    eyebrow: 'SESSION EXPIRED',
    title: 'Let’s reconnect',
    actions: [AppButton(label: 'Log in again', onPressed: onLogin)],
    children: [
      Container(
        width: double.infinity,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: AppTheme.sessionGradient,
          borderRadius: BorderRadius.circular(40),
        ),
        child: Center(
          child: SizedBox.square(
            dimension: 232,
            child: Stack(
              children: [
                Positioned(
                  left: 6,
                  top: 6,
                  width: 220,
                  height: 220,
                  child: SvgPicture.asset('assets/images/session_track.svg'),
                ),
                Positioned(
                  left: 6,
                  top: 6,
                  width: 220,
                  height: 220,
                  child: SvgPicture.asset('assets/images/session_progress.svg'),
                ),
                Positioned(
                  top: 72,
                  left: 0,
                  right: 0,
                  child: Text(
                    '↻',
                    textAlign: TextAlign.center,
                    style: AppTheme.textStyle(
                      52,
                      weight: FontWeight.w700,
                      color: AppColors.white,
                      height: 1,
                    ),
                  ),
                ),
                Positioned(
                  top: 139,
                  left: 0,
                  right: 0,
                  child: Text(
                    'Your diary is safe',
                    textAlign: TextAlign.center,
                    style: AppTheme.textStyle(
                      13,
                      weight: FontWeight.w500,
                      color: AppColors.white,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
      Text(
        'Your session has expired.',
        style: Theme.of(context).textTheme.headlineSmall,
      ),
      const Text(
        'For your security, please log in again. Your saved meals and nutrition logs are still here.',
      ),
    ],
  );
}
