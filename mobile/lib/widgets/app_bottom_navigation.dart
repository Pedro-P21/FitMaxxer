import 'package:material_ui/material_ui.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../theme/app_colors.dart';
import '../theme/app_theme.dart';

class AppBottomNavigation extends StatelessWidget {
  const AppBottomNavigation({
    super.key,
    required this.selected,
    required this.onSelect,
  });
  final String selected;
  final ValueChanged<String> onSelect;

  @override
  Widget build(BuildContext context) => SafeArea(
    top: false,
    child: Container(
      color: AppColors.white,
      padding: const EdgeInsets.fromLTRB(12, 10, 12, 18),
      child: Row(
        children: [
          for (final tab in ['Today', 'History', 'Recipes', 'Profile'])
            Expanded(
              child: Semantics(
                selected: tab == selected,
                child: InkWell(
                  onTap: tab == 'Today' ? () => onSelect(tab) : null,
                  borderRadius: BorderRadius.circular(16),
                  child: Container(
                    padding: const EdgeInsets.all(4),
                    decoration: BoxDecoration(
                      color: tab == selected ? AppColors.pale : AppColors.white,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        SvgPicture.asset(
                          'assets/images/${tab.toLowerCase()}.svg',
                          width: 24,
                          height: 24,
                          colorFilter: ColorFilter.mode(
                            tab == selected ? AppColors.blue : AppColors.muted,
                            BlendMode.srcIn,
                          ),
                        ),
                        const SizedBox(height: 3),
                        Text(
                          tab,
                          style: AppTheme.textStyle(
                            10,
                            weight: FontWeight.w600,
                            color: tab == selected
                                ? AppColors.blue
                                : AppColors.muted,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    ),
  );
}
