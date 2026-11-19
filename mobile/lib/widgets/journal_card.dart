import 'package:material_ui/material_ui.dart';

import '../theme/app_colors.dart';

class JournalCard extends StatelessWidget {
  const JournalCard({
    super.key,
    required this.child,
    this.onTap,
    this.selected = false,
    this.outlined = false,
  });
  final Widget child;
  final VoidCallback? onTap;
  final bool selected, outlined;
  @override
  Widget build(BuildContext context) => SizedBox(
    width: double.infinity,
    child: Material(
      color: selected ? AppColors.pale : AppColors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(22),
        side: outlined
            ? const BorderSide(color: AppColors.line)
            : BorderSide.none,
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(padding: const EdgeInsets.all(16), child: child),
      ),
    ),
  );
}
