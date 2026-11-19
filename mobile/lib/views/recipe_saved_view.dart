import 'package:material_ui/material_ui.dart';

import '../models/nutrition.dart';
import '../models/recipe.dart';
import '../providers/daily_log_provider.dart';
import '../services/recipe_service.dart';
import '../widgets/app_button.dart';
import '../widgets/app_feedback_card.dart';
import '../widgets/flow_scaffold.dart';
import '../widgets/nutrition_card.dart';
import 'recipe_details_view.dart';

class RecipeSavedView extends StatelessWidget {
  const RecipeSavedView({
    super.key,
    required this.recipe,
    required this.service,
    required this.diary,
  });
  final RecipeDetails recipe;
  final RecipeService service;
  final DailyLogProvider diary;
  Future<void> _view(BuildContext context) async {
    final logged = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => RecipeDetailsView(
          id: recipe.id,
          initialRecipe: recipe,
          service: service,
          diary: diary,
        ),
      ),
    );
    if (logged == true && context.mounted) Navigator.pop(context, true);
  }

  @override
  Widget build(BuildContext context) => FlowScaffold(
    eyebrow: 'RECIPE SAVED',
    title: 'Ready for your next meal',
    actions: [
      AppButton(label: 'View recipe', onPressed: () => _view(context)),
      AppButton(
        label: 'Back to Recipes',
        variant: AppButtonVariant.secondary,
        onPressed: () => Navigator.pop(context, false),
      ),
    ],
    children: [
      AppFeedbackCard(
        title: '${recipe.name} is saved',
        detail:
            '${amountText(recipe.servings)} servings · ${amountText(recipe.perServing.calories)} kcal per serving',
      ),
      NutritionCard(
        nutrients: recipe.perServing,
        precise: true,
        label: 'PER SERVING',
      ),
      const Text(
        'Your recipe is now in Recipes. Log a whole serving or just a fraction whenever you like.',
      ),
    ],
  );
}
