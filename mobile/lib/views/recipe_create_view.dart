import 'package:material_ui/material_ui.dart';

import '../models/recipe.dart';
import '../providers/daily_log_provider.dart';
import '../providers/recipes_provider.dart';
import '../services/food_catalog_service.dart';
import '../widgets/app_button.dart';
import '../widgets/app_feedback_card.dart';
import '../widgets/app_text_field.dart';
import '../widgets/flow_scaffold.dart';
import '../widgets/ingredient_editor.dart';
import '../widgets/journal_card.dart';
import 'food_search_view.dart';
import 'recipe_saved_view.dart';
import 'recipe_yield_view.dart';

class RecipeCreateView extends StatefulWidget {
  const RecipeCreateView({
    super.key,
    required this.recipes,
    required this.diary,
    this.scanBarcode,
  });
  final RecipesProvider recipes;
  final DailyLogProvider diary;
  final BarcodeScan? scanBarcode;
  @override
  State<RecipeCreateView> createState() => _RecipeCreateViewState();
}

class _RecipeCreateViewState extends State<RecipeCreateView> {
  final _draft = RecipeDraft();
  final _name = TextEditingController();
  final _form = GlobalKey<FormState>();
  bool _choosing = false, _continuing = false;
  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  Future<void> _addIngredient() async {
    if (_choosing) return;
    FocusScope.of(context).unfocus();
    setState(() => _choosing = true);
    final ingredient = await Navigator.of(context).push<DraftIngredient>(
      MaterialPageRoute(
        builder: (_) => FoodSearchView(
          catalog: FoodCatalogService(widget.recipes.service.api),
          diary: widget.diary,
          meal: 'Breakfast',
          selectIngredient: true,
          scanBarcode: widget.scanBarcode,
        ),
      ),
    );
    if (mounted) {
      setState(() {
        _choosing = false;
        if (ingredient != null) _draft.ingredients.add(ingredient);
      });
    }
  }

  Future<void> _next() async {
    if (_continuing || !_form.currentState!.validate() || _draft.total == null) {
      return;
    }
    FocusScope.of(context).unfocus();
    _draft.name = _name.text.trim();
    setState(() => _continuing = true);
    final recipe = await Navigator.of(context).push<RecipeDetails>(
      MaterialPageRoute(
        builder: (_) => RecipeYieldView(draft: _draft, recipes: widget.recipes),
      ),
    );
    if (!mounted) return;
    if (recipe == null) {
      setState(() => _continuing = false);
      return;
    }
    final logged = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => RecipeSavedView(
          recipe: recipe,
          service: widget.recipes.service,
          diary: widget.diary,
        ),
      ),
    );
    if (mounted) Navigator.pop(context, logged ?? false);
  }

  @override
  Widget build(BuildContext context) => FlowScaffold(
    eyebrow: '01 / 02 · INGREDIENTS',
    title: 'Create a recipe',
    backLabel: 'Recipes',
    onBack: () => Navigator.pop(context),
    actions: [
      AppButton(
        label: 'Continue to yield',
        onPressed: _draft.total == null || _continuing ? null : _next,
      ),
    ],
    children: [
      Form(
        key: _form,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            AppTextField(
              label: 'Recipe name',
              controller: _name,
              validator: (v) =>
                  v == null || v.trim().isEmpty || v.trim().length > 255
                  ? 'Use a name from 1 to 255 characters.'
                  : null,
            ),
            const SizedBox(height: 14),
            Text('Ingredients', style: Theme.of(context).textTheme.titleMedium),
            if (_draft.ingredients.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 12),
                child: Text(
                  'Add at least one catalog ingredient to get started.',
                ),
              ),
            for (final ingredient in _draft.ingredients)
              Padding(
                key: ObjectKey(ingredient),
                padding: const EdgeInsets.symmetric(vertical: 10),
                child: Column(
                  children: [
                    JournalCard(
                      outlined: true,
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              ingredient.food.name,
                              style: Theme.of(context).textTheme.titleMedium,
                            ),
                          ),
                          IconButton(
                            tooltip: 'Remove ${ingredient.food.name}',
                            onPressed: () => setState(
                              () => _draft.ingredients.remove(ingredient),
                            ),
                            icon: const Icon(Icons.close_rounded),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 10),
                    IngredientEditor(
                      ingredient: ingredient,
                      onChanged: () => setState(() {}),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
      AppButton(
        label: '+ Add catalog ingredient',
        variant: AppButtonVariant.secondary,
        onPressed: _choosing ? null : _addIngredient,
      ),
      const AppFeedbackCard(
        title: 'Build it from the catalog',
        detail: 'Nutrition is calculated from each ingredient’s amount.',
      ),
    ],
  );
}
