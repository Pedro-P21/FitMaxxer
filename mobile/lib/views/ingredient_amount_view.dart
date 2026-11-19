import 'package:material_ui/material_ui.dart';

import '../models/catalog_food.dart';
import '../models/nutrition.dart';
import '../models/recipe.dart';
import '../widgets/app_button.dart';
import '../widgets/flow_scaffold.dart';
import '../widgets/ingredient_editor.dart';
import '../widgets/journal_card.dart';
import '../widgets/nutrition_card.dart';

class IngredientAmountView extends StatefulWidget {
  const IngredientAmountView({super.key, required this.food});
  final CatalogFood food;
  @override
  State<IngredientAmountView> createState() => _IngredientAmountViewState();
}

class _IngredientAmountViewState extends State<IngredientAmountView> {
  DraftIngredient? _ingredient;
  @override
  void initState() {
    super.initState();
    if (widget.food.nutrition.units.isNotEmpty) {
      _ingredient = DraftIngredient(
        food: widget.food,
        quantity: widget.food.nutrition.units.first == 'SERVING' ? '1' : '100',
      );
    }
  }

  @override
  Widget build(BuildContext context) => FlowScaffold(
    eyebrow: 'RECIPE BUILDER',
    title: 'Add an ingredient',
    backLabel: 'Search food',
    onBack: () => Navigator.pop(context),
    actions: [
      AppButton(
        label: 'Add ingredient',
        onPressed: _ingredient?.contribution == null
            ? null
            : () => Navigator.pop(context, _ingredient),
      ),
    ],
    children: [
      JournalCard(
        outlined: true,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              widget.food.name,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            Text(
              '${amountText(widget.food.nutrition.nutrients.calories)} kcal / ${widget.food.nutrition.basisLabel}',
            ),
          ],
        ),
      ),
      if (_ingredient case final ingredient?) ...[
        IngredientEditor(
          ingredient: ingredient,
          onChanged: () => setState(() {}),
        ),
        NutritionCard(
          nutrients: ingredient.contribution,
          precise: true,
          label: 'ADDED NUTRITION FOR THIS AMOUNT',
        ),
      ] else
        const InlineNotice(
          'No compatible units are available for this ingredient.',
          error: true,
        ),
    ],
  );
}
