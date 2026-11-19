import 'package:material_ui/material_ui.dart';

import '../models/daily_log.dart';
import '../models/nutrition.dart';
import '../models/recipe.dart';
import '../providers/daily_log_provider.dart';
import '../widgets/app_button.dart';
import '../widgets/app_select_field.dart';
import '../widgets/app_text_field.dart';
import '../widgets/flow_scaffold.dart';
import '../widgets/journal_card.dart';
import '../widgets/nutrition_card.dart';

class RecipeLogView extends StatefulWidget {
  const RecipeLogView({super.key, required this.recipe, required this.diary});
  final RecipeSummary recipe;
  final DailyLogProvider diary;
  @override
  State<RecipeLogView> createState() => _RecipeLogViewState();
}

class _RecipeLogViewState extends State<RecipeLogView> {
  final _quantity = TextEditingController(text: '1');
  String _meal = 'Breakfast';
  @override
  void dispose() {
    _quantity.dispose();
    super.dispose();
  }

  Future<void> _log() async {
    if (quantityError(_quantity.text) != null || widget.diary.busy) return;
    final saved = await widget.diary.addRecipe(
      widget.recipe.id,
      widget.recipe.name,
      _meal,
      double.parse(_quantity.text.trim().replaceAll(',', '.')),
    );
    if (saved && mounted) Navigator.pop(context, true);
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: widget.diary,
    builder: (context, _) {
      final valid = quantityError(_quantity.text) == null;
      final quantity =
          double.tryParse(_quantity.text.trim().replaceAll(',', '.')) ?? 0;
      final preview = valid ? widget.recipe.perServing.scaled(quantity) : null;
      final busy = widget.diary.busy;
      return PopScope(
        canPop: !busy,
        child: FlowScaffold(
          eyebrow: 'LOG A RECIPE',
          title: 'A little or a lot?',
          backLabel: 'Recipe details',
          onBack: busy ? null : () => Navigator.pop(context),
          actions: [
            AppButton(
              label: valid
                  ? 'Log ${amountText(quantity)} ${quantity == 1 ? 'serving' : 'servings'} · ${preview!.calories.round()} kcal'
                  : 'Correct quantity',
              isLoading: busy,
              onPressed: valid ? _log : null,
            ),
          ],
          children: [
            JournalCard(
              outlined: true,
              child: Text(
                widget.recipe.name,
                style: Theme.of(context).textTheme.titleMedium,
              ),
            ),
            AppTextField(
              label: 'Number of servings',
              controller: _quantity,
              enabled: !busy,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              autovalidateMode: AutovalidateMode.onUserInteraction,
              validator: quantityError,
              onChanged: (_) => setState(() {}),
            ),
            const Text('Fractions welcome · log any part of one serving.'),
            AppSelectField(
              label: 'Add to meal',
              value: _meal,
              options: {for (final meal in mealTypes) meal: meal},
              onChanged: busy ? null : (v) => setState(() => _meal = v),
            ),
            NutritionCard(
              nutrients: preview,
              precise: true,
              label: valid
                  ? 'FOR ${amountText(quantity)} ${quantity == 1 ? 'SERVING' : 'SERVINGS'}'
                  : 'NUTRITION FOR THIS PORTION',
            ),
            if (widget.diary.error != null)
              InlineNotice(widget.diary.error!, error: true),
          ],
        ),
      );
    },
  );
}
