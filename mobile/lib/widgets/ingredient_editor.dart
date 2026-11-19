import 'package:material_ui/material_ui.dart';

import '../models/nutrition.dart';
import '../models/recipe.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import 'app_select_field.dart';
import 'app_text_field.dart';

class IngredientEditor extends StatefulWidget {
  const IngredientEditor({
    super.key,
    required this.ingredient,
    required this.onChanged,
  });
  final DraftIngredient ingredient;
  final VoidCallback onChanged;
  @override
  State<IngredientEditor> createState() => _IngredientEditorState();
}

class _IngredientEditorState extends State<IngredientEditor> {
  late final TextEditingController _quantity;
  @override
  void initState() {
    super.initState();
    _quantity = TextEditingController(text: widget.ingredient.quantity);
  }

  @override
  void dispose() {
    _quantity.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: AppTextField(
              label: 'Quantity',
              controller: _quantity,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              autovalidateMode: AutovalidateMode.onUserInteraction,
              validator: quantityError,
              onChanged: (v) {
                widget.ingredient.quantity = v;
                widget.onChanged();
              },
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: AppSelectField(
              label: 'Unit',
              value: widget.ingredient.unit,
              options: {
                for (final unit in widget.ingredient.food.nutrition.units)
                  unit: unitLabel(unit),
              },
              onChanged: (v) {
                widget.ingredient.unit = v;
                widget.onChanged();
              },
            ),
          ),
        ],
      ),
      const SizedBox(height: 8),
      Text(
        'Compatible: ${widget.ingredient.food.nutrition.units.map(unitLabel).join(', ')}.'
        '${widget.ingredient.food.nutrition.servingQuantity == null ? '' : ' Catalog serving: ${amountText(widget.ingredient.food.nutrition.servingQuantity!)} ${widget.ingredient.food.nutrition.servingUnit}.'}',
        style: AppTheme.textStyle(11, color: AppColors.muted),
      ),
    ],
  );
}
