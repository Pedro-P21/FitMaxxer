import 'package:material_ui/material_ui.dart';

import '../models/nutrition.dart';
import '../models/recipe.dart';
import '../providers/daily_log_provider.dart';
import '../services/api_client.dart';
import '../services/recipe_service.dart';
import '../widgets/app_button.dart';
import '../widgets/flow_scaffold.dart';
import '../widgets/journal_card.dart';
import '../widgets/nutrition_card.dart';
import 'recipe_log_view.dart';

class RecipeDetailsView extends StatefulWidget {
  const RecipeDetailsView({
    super.key,
    required this.id,
    required this.service,
    required this.diary,
    this.initialRecipe,
  });
  final String id;
  final RecipeService service;
  final DailyLogProvider diary;
  final RecipeDetails? initialRecipe;
  @override
  State<RecipeDetailsView> createState() => _RecipeDetailsViewState();
}

class _RecipeDetailsViewState extends State<RecipeDetailsView> {
  RecipeDetails? _recipe;
  bool _loading = false;
  String? _error;
  @override
  void initState() {
    super.initState();
    _recipe = widget.initialRecipe;
    if (_recipe == null) _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final recipe = await widget.service.get(widget.id);
      if (mounted) setState(() => _recipe = recipe);
    } catch (e) {
      if (mounted) {
        setState(
          () => _error = e is ApiException
              ? e.message
              : 'This recipe could not be loaded. Please try again.',
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _log() async {
    widget.diary.clearFeedback();
    final logged = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => RecipeLogView(recipe: _recipe!, diary: widget.diary),
      ),
    );
    if (logged == true && mounted) Navigator.pop(context, true);
  }

  @override
  Widget build(BuildContext context) {
    final recipe = _recipe;
    return FlowScaffold(
      eyebrow: recipe == null
          ? 'YOUR RECIPES'
          : 'SAVED RECIPE · ${amountText(recipe.servings)} SERVINGS',
      title: recipe?.name ?? 'Your recipe',
      backLabel: 'Recipes',
      onBack: () => Navigator.pop(context),
      actions: [
        if (recipe != null)
          AppButton(
            label: 'Log this recipe',
            onPressed: _loading ? null : _log,
          ),
      ],
      children: [
        if (_loading) const Center(child: CircularProgressIndicator()),
        if (_error != null) ...[
          InlineNotice(_error!, error: true),
          AppButton(label: 'Try again', onPressed: _load),
        ],
        if (recipe != null) ...[
          NutritionCard(
            nutrients: recipe.perServing,
            precise: true,
            label: 'PER SERVING',
          ),
          Text(
            'The ingredients',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          for (final item in recipe.ingredients)
            JournalCard(
              outlined: true,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${item.name} · ${amountText(item.quantity)} ${unitLabel(item.unit, short: true)}',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 6),
                  Text(
                    item.contribution == null
                        ? 'Nutrition unavailable'
                        : '${amountText(item.contribution!.calories)} kcal · P ${amountText(item.contribution!.protein)} g · C ${amountText(item.contribution!.carbs)} g · F ${amountText(item.contribution!.fat)} g',
                  ),
                ],
              ),
            ),
          Text(
            'Whole recipe: ${amountText(recipe.total.calories)} kcal\nOriginal ingredients and yield are kept as saved.',
          ),
        ],
      ],
    );
  }
}
