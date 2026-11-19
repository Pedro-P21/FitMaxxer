import 'package:material_ui/material_ui.dart';

import '../models/nutrition.dart';
import '../models/recipe.dart';
import '../providers/daily_log_provider.dart';
import '../providers/recipes_provider.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_bottom_navigation.dart';
import '../widgets/app_button.dart';
import '../widgets/journal_card.dart';
import '../widgets/nutrition_card.dart';
import 'food_search_view.dart';
import 'recipe_create_view.dart';
import 'recipe_details_view.dart';
import 'recipe_log_view.dart';

class RecipesView extends StatefulWidget {
  const RecipesView({
    super.key,
    required this.recipes,
    required this.diary,
    required this.onSelectTab,
    required this.onLogged,
    this.scanBarcode,
  });
  final RecipesProvider recipes;
  final DailyLogProvider diary;
  final ValueChanged<String> onSelectTab;
  final VoidCallback onLogged;
  final BarcodeScan? scanBarcode;
  @override
  State<RecipesView> createState() => _RecipesViewState();
}

class _RecipesViewState extends State<RecipesView> {
  bool _opening = false;
  @override
  void initState() {
    super.initState();
    if ((!widget.recipes.loaded || widget.recipes.dirty) &&
        !widget.recipes.loading) {
      widget.recipes.load();
    }
  }

  Future<void> _create() async {
    if (_opening) return;
    _opening = true;
    final logged = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => RecipeCreateView(
          recipes: widget.recipes,
          diary: widget.diary,
          scanBarcode: widget.scanBarcode,
        ),
      ),
    );
    if (!mounted) return;
    _opening = false;
    if (logged == true) {
      widget.onLogged();
    } else if (widget.recipes.dirty) {
      widget.recipes.load();
    }
  }

  Future<void> _open(RecipeSummary recipe, {bool quick = false}) async {
    if (_opening || widget.diary.busy) return;
    _opening = true;
    widget.diary.clearFeedback();
    final logged = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => quick
            ? RecipeLogView(recipe: recipe, diary: widget.diary)
            : RecipeDetailsView(
                id: recipe.id,
                service: widget.recipes.service,
                diary: widget.diary,
              ),
      ),
    );
    if (!mounted) return;
    _opening = false;
    if (logged == true) widget.onLogged();
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: widget.recipes,
    builder: (context, _) {
      final recipes = widget.recipes;
      return Scaffold(
        bottomNavigationBar: AppBottomNavigation(
          selected: 'Recipes',
          onSelect: widget.onSelectTab,
        ),
        body: SafeArea(
          child: RefreshIndicator(
            onRefresh: recipes.load,
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(20),
              children: [
                Text(
                  'YOUR RECIPES',
                  style: AppTheme.textStyle(
                    10,
                    color: AppColors.blue,
                    weight: FontWeight.w600,
                  ),
                ),
                Text(
                  'Cook once. Log again.',
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: 14),
                const Text('Your own combinations, ready whenever you are.'),
                const SizedBox(height: 14),
                if (recipes.loading && recipes.items.isEmpty)
                  const Center(child: CircularProgressIndicator()),
                if (recipes.loaded &&
                    recipes.items.isEmpty &&
                    recipes.error == null &&
                    !recipes.loading)
                  const JournalCard(
                    child: Text(
                      'No recipes yet. Create your first recipe to make logging easier.',
                    ),
                  ),
                for (final recipe in recipes.items) ...[
                  Dismissible(
                    key: ValueKey('swipe-recipe-${recipe.id}'),
                    direction: DismissDirection.endToStart,
                    dismissThresholds: const {DismissDirection.endToStart: .4},
                    background: Container(
                      color: AppColors.success,
                      alignment: Alignment.centerRight,
                      padding: const EdgeInsets.only(right: 20),
                      child: const Icon(
                        Icons.add,
                        color: AppColors.white,
                        size: 28,
                      ),
                    ),
                    confirmDismiss: (_) async {
                      await _open(recipe, quick: true);
                      return false;
                    },
                    child: JournalCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            recipe.name,
                            style: AppTheme.textStyle(
                              20,
                              weight: FontWeight.w700,
                            ),
                          ),
                          Text('Makes ${amountText(recipe.servings)} servings'),
                          const SizedBox(height: 10),
                          Text(
                            '${amountText(recipe.perServing.calories)} kcal / serving',
                            style: AppTheme.textStyle(
                              20,
                              weight: FontWeight.w700,
                              color: AppColors.blue,
                            ),
                          ),
                          Text(
                            'P ${amountText(recipe.perServing.protein)} g · C ${amountText(recipe.perServing.carbs)} g · F ${amountText(recipe.perServing.fat)} g',
                            style: AppTheme.textStyle(
                              12,
                              color: AppColors.muted,
                            ),
                          ),
                          TextButton(
                            key: ValueKey('view-recipe-${recipe.id}'),
                            onPressed: () => _open(recipe),
                            child: const Text('View recipe →'),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 14),
                ],
                if (recipes.error != null) ...[
                  InlineNotice(recipes.error!, error: true),
                  AppButton(
                    label: 'Try again',
                    onPressed: () => recipes.load(more: recipes.retryMore),
                  ),
                ] else if (recipes.hasMore)
                  AppButton(
                    label: 'Load more recipes',
                    variant: AppButtonVariant.secondary,
                    isLoading: recipes.loading,
                    onPressed: () => recipes.load(more: true),
                  ),
                const SizedBox(height: 14),
                AppButton(label: '+ Create a recipe', onPressed: _create),
                const SizedBox(height: 14),
                const Text(
                  'Saved recipes keep their original ingredients and nutrition. Create a new recipe for a different version.',
                ),
              ],
            ),
          ),
        ),
      );
    },
  );
}
