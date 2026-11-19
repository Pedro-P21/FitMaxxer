import 'package:material_ui/material_ui.dart';

import '../theme/app_colors.dart';
import '../theme/app_theme.dart';

class AppSelectField extends StatelessWidget {
  const AppSelectField({
    super.key,
    required this.label,
    required this.value,
    required this.options,
    required this.onChanged,
  });
  final String label, value;
  final Map<String, String> options;
  final ValueChanged<String>? onChanged;
  @override
  Widget build(BuildContext context) => Container(
    constraints: const BoxConstraints(minHeight: 74),
    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
    decoration: BoxDecoration(
      color: AppColors.white,
      border: Border.all(color: AppColors.line),
      borderRadius: BorderRadius.circular(AppTheme.componentRadius),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: AppTheme.textStyle(
            11,
            weight: FontWeight.w500,
            color: AppColors.muted,
          ),
        ),
        DropdownButtonHideUnderline(
          child: DropdownButton<String>(
            value: value,
            isExpanded: true,
            isDense: true,
            borderRadius: BorderRadius.circular(22),
            dropdownColor: AppColors.white,
            elevation: 4,
            menuMaxHeight: 320,
            icon: const Icon(
              Icons.keyboard_arrow_down_rounded,
              color: AppColors.blue,
            ),
            selectedItemBuilder: (_) => [
              for (final text in options.values)
                Align(alignment: Alignment.centerLeft, child: Text(text)),
            ],
            style: AppTheme.textStyle(16, weight: FontWeight.w500),
            items: options.entries
                .map(
                  (e) => DropdownMenuItem(
                    value: e.key,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 8,
                      ),
                      decoration: BoxDecoration(
                        color: e.key == value
                            ? AppColors.pale
                            : Colors.transparent,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              e.value,
                              style: AppTheme.textStyle(
                                15,
                                weight: e.key == value
                                    ? FontWeight.w600
                                    : FontWeight.w400,
                                color: e.key == value
                                    ? AppColors.blue
                                    : AppColors.ink,
                              ),
                            ),
                          ),
                          if (e.key == value) ...[
                            const SizedBox(width: 8),
                            const Icon(
                              Icons.check_rounded,
                              size: 18,
                              color: AppColors.blue,
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),
                )
                .toList(),
            onChanged: onChanged == null
                ? null
                : (v) {
                    if (v != null) onChanged!(v);
                  },
          ),
        ),
      ],
    ),
  );
}
