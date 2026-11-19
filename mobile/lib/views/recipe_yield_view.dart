import 'package:material_ui/material_ui.dart';

import '../models/nutrition.dart';
import '../models/recipe.dart';
import '../providers/recipes_provider.dart';
import '../widgets/app_button.dart';
import '../widgets/app_feedback_card.dart';
import '../widgets/app_text_field.dart';
import '../widgets/flow_scaffold.dart';
import '../widgets/journal_card.dart';
import '../widgets/nutrition_card.dart';

class RecipeYieldView extends StatefulWidget {
  const RecipeYieldView({
    super.key,
    required this.draft,
    required this.recipes,
  });
  final RecipeDraft draft;
  final RecipesProvider recipes;
  @override
  State<RecipeYieldView> createState() => _RecipeYieldViewState();
}

class _RecipeYieldViewState extends State<RecipeYieldView> {
  late final TextEditingController _servings;
  @override
  void initState() {
    super.initState();
    _servings = TextEditingController(text: widget.draft.servings);
    widget.recipes.clearSaveError();
  }

  @override
  void dispose() {
    _servings.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (widget.draft.perServing == null || widget.recipes.saving) return;
    final result = await widget.recipes.create(widget.draft);
    if (result != null && mounted) Navigator.pop(context, result);
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: widget.recipes,
    builder: (context, _) {
      final draft = widget.draft, busy = widget.recipes.saving;
      return PopScope(
        canPop: !busy,
        child: FlowScaffold(
          eyebrow: '02 / 02 · SERVING YIELD',
          title: 'How much does it make?',
          backLabel: 'Create recipe',
          onBack: busy ? null : () => Navigator.pop(context),
          actions: [
            AppButton(
              label: 'Save recipe',
              isLoading: busy,
              onPressed: draft.perServing == null ? null : _save,
            ),
          ],
          children: [
            JournalCard(
              outlined: true,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    draft.name,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  Text(
                    draft.ingredients
                        .map(
                          (i) =>
                              '${amountText(i.amount)} ${unitLabel(i.unit, short: true)} ${i.food.name}',
                        )
                        .join(' + '),
                  ),
                ],
              ),
            ),
            AppTextField(
              label: 'Number of servings',
              controller: _servings,
              enabled: !busy,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              autovalidateMode: AutovalidateMode.onUserInteraction,
              validator: quantityError,
              onChanged: (v) => setState(() => draft.servings = v),
            ),
            const Text(
              'Yield must be greater than zero. Fractional yields are allowed.',
            ),
            NutritionCard(
              nutrients: draft.perServing,
              precise: true,
              label: 'PER SERVING · RECIPE ÷ ${draft.servings}',
            ),
            Text(
              'Whole recipe: ${amountText(draft.total!.calories)} kcal\nP ${amountText(draft.total!.protein)} g · C ${amountText(draft.total!.carbs)} g · F ${amountText(draft.total!.fat)} g',
            ),
            const AppFeedbackCard(
              title: 'Check it before saving',
              detail: 'Saved recipes can be reused, but not edited or deleted.',
            ),
            if (widget.recipes.saveError != null)
              InlineNotice(widget.recipes.saveError!, error: true),
          ],
        ),
      );
    },
  );
}
