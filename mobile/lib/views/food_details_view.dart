import 'package:material_ui/material_ui.dart';

import '../models/catalog_food.dart';
import '../models/daily_log.dart';
import '../models/nutrition.dart';
import '../providers/daily_log_provider.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_button.dart';
import '../widgets/app_select_field.dart';
import '../widgets/app_text_field.dart';
import '../widgets/flow_scaffold.dart';
import '../widgets/nutrition_card.dart';

class FoodDetailsView extends StatefulWidget {
  const FoodDetailsView({
    super.key,
    required this.food,
    required this.diary,
    required this.meal,
  });
  final CatalogFood food;
  final DailyLogProvider diary;
  final String meal;
  @override
  State<FoodDetailsView> createState() => _FoodDetailsViewState();
}

class _FoodDetailsViewState extends State<FoodDetailsView> {
  final _quantity = TextEditingController(text: '1');
  final _form = GlobalKey<FormState>();
  late String _unit, _meal;
  double get multiplier => _unit.startsWith('100_') ? 100 : 1;
  String get apiUnit => _unit.replaceFirst('100_', '');
  double get amount =>
      (double.tryParse(_quantity.text.trim().replaceAll(',', '.')) ?? 0) *
      multiplier;
  Map<String, String> get options => {
    for (final unit in widget.food.nutrition.units) ...{
      if (unit != 'SERVING') '100_$unit': '100 ${unitLabel(unit, short: true)}',
      unit: unit == 'SERVING' ? 'Serving' : unitLabel(unit),
    },
  };
  @override
  void initState() {
    super.initState();
    _meal = widget.meal;
    _unit = options.keys.firstOrNull ?? 'SERVING';
  }

  @override
  void dispose() {
    _quantity.dispose();
    super.dispose();
  }

  Future<void> _add() async {
    FocusScope.of(context).unfocus();
    if (!_form.currentState!.validate() || options.isEmpty) return;
    final success = await widget.diary.add(
      widget.food.id,
      _meal,
      double.parse(amount.toStringAsFixed(4)),
      apiUnit,
    );
    if (success && mounted) Navigator.of(context).pop(true);
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: widget.diary,
    builder: (context, _) {
      final nutrition =
          quantityError(_quantity.text, multiplier: multiplier) == null
          ? widget.food.nutrition.forQuantity(amount, apiUnit)
          : null;
      final busy = widget.diary.busy;
      return PopScope(
        canPop: !busy,
        child: Form(
          key: _form,
          child: FlowScaffold(
            eyebrow:
                '${widget.food.brand?.toUpperCase() ?? 'FOOD'} · CATALOG FOOD',
            title: widget.food.name,
            backLabel: 'Search food',
            onBack: busy ? null : () => Navigator.pop(context),
            actions: [
              AppButton(
                label: nutrition == null
                    ? 'Correct quantity'
                    : 'Add ${nutrition.calories.round()} kcal to $_meal',
                isLoading: busy,
                onPressed: nutrition == null || options.isEmpty ? null : _add,
              ),
            ],
            children: [
              NutritionCard(nutrients: nutrition),
              Text(
                'Per ${widget.food.nutrition.basisLabel}: '
                '${amountText(widget.food.nutrition.nutrients.calories)} kcal · '
                'P ${amountText(widget.food.nutrition.nutrients.protein)} g · '
                'C ${amountText(widget.food.nutrition.nutrients.carbs)} g · '
                'F ${amountText(widget.food.nutrition.nutrients.fat)} g',
                style: AppTheme.textStyle(12, color: AppColors.muted),
              ),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: AppTextField(
                      label: 'Quantity',
                      controller: _quantity,
                      enabled: !busy,
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                      textInputAction: TextInputAction.done,
                      autovalidateMode: AutovalidateMode.onUserInteraction,
                      onChanged: (_) => setState(() {}),
                      validator: (v) =>
                          quantityError(v, multiplier: multiplier),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: options.isEmpty
                        ? const InlineNotice(
                            'No compatible units are available.',
                            error: true,
                          )
                        : AppSelectField(
                            label: 'Unit',
                            value: _unit,
                            options: options,
                            onChanged: busy
                                ? null
                                : (v) => setState(() => _unit = v),
                          ),
                  ),
                ],
              ),
              if (nutrition != null && multiplier == 100)
                Text(
                  '${_quantity.text} × 100 ${unitLabel(apiUnit, short: true)} = ${amountText(amount)} ${unitLabel(apiUnit, short: true)} · fractions welcome',
                  style: AppTheme.textStyle(12, color: AppColors.blue),
                ),
              AppSelectField(
                label: 'Add to meal',
                value: _meal,
                options: {for (final meal in mealTypes) meal: meal},
                onChanged: busy ? null : (v) => setState(() => _meal = v),
              ),
              Text(
                'Compatible units: ${widget.food.nutrition.units.map((u) => unitLabel(u)).join(', ')}.'
                '${widget.food.nutrition.units.contains('SERVING') && widget.food.nutrition.servingQuantity != null ? ' Catalog serving: ${amountText(widget.food.nutrition.servingQuantity!)} ${widget.food.nutrition.servingUnit}.' : ''}',
                style: AppTheme.textStyle(12, color: AppColors.muted),
              ),
              if (widget.diary.error != null)
                InlineNotice(widget.diary.error!, error: true),
            ],
          ),
        ),
      );
    },
  );
}
