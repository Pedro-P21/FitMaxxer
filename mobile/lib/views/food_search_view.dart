import 'package:material_ui/material_ui.dart';

import '../models/catalog_food.dart';
import '../models/recipe.dart';
import 'ingredient_amount_view.dart';
import '../providers/daily_log_provider.dart';
import '../providers/food_catalog_provider.dart';
import '../services/food_catalog_service.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_button.dart';
import '../widgets/app_feedback_card.dart';
import '../widgets/app_text_field.dart';
import '../widgets/flow_scaffold.dart';
import 'barcode_confirmation_view.dart';
import 'barcode_scanner_view.dart';
import 'food_details_view.dart';

typedef BarcodeScan = Future<String?> Function(BuildContext context);

class FoodSearchView extends StatefulWidget {
  const FoodSearchView({
    super.key,
    required this.catalog,
    required this.diary,
    required this.meal,
    this.scanBarcode,
    this.selectIngredient = false,
  });
  final FoodCatalogService catalog;
  final DailyLogProvider diary;
  final String meal;
  final BarcodeScan? scanBarcode;
  final bool selectIngredient;
  @override
  State<FoodSearchView> createState() => _FoodSearchViewState();
}

class _FoodSearchViewState extends State<FoodSearchView> {
  late final FoodCatalogProvider _catalog;
  final _query = TextEditingController();
  final _searchFocus = FocusNode();
  bool _scanning = false;
  @override
  void initState() {
    super.initState();
    _catalog = FoodCatalogProvider(widget.catalog);
  }

  @override
  void dispose() {
    _query.dispose();
    _searchFocus.dispose();
    _catalog.dispose();
    super.dispose();
  }

  Future<void> _scan() async {
    if (_scanning) return;
    FocusScope.of(context).unfocus();
    setState(() => _scanning = true);
    try {
      final code =
          await (widget.scanBarcode?.call(context) ??
              Navigator.of(context).push<String>(
                MaterialPageRoute(builder: (_) => const BarcodeScannerView()),
              ));
      if (!mounted || code == null) return;
      final added = await Navigator.of(context).push<Object>(
        MaterialPageRoute(
          builder: (_) => BarcodeConfirmationView(
            barcode: code,
            selectIngredient: widget.selectIngredient,
            catalog: widget.catalog,
            diary: widget.diary,
            meal: widget.meal,
          ),
        ),
      );
      if (mounted && (added == true || added is DraftIngredient)) {
        Navigator.pop(context, added);
      }
    } finally {
      if (mounted) setState(() => _scanning = false);
    }
  }

  Future<void> _select(CatalogFood food) async {
    if (!widget.selectIngredient) widget.diary.clearFeedback();
    final added = await Navigator.of(context).push<Object>(
      MaterialPageRoute(
        builder: (_) => widget.selectIngredient
            ? IngredientAmountView(food: food)
            : FoodDetailsView(
                food: food,
                diary: widget.diary,
                meal: widget.meal,
              ),
      ),
    );
    if (mounted && (added == true || added is DraftIngredient)) {
      Navigator.pop(context, added);
    }
  }

  void _today() => Navigator.of(context).popUntil((route) => route.isFirst);

  void _search() {
    if (_query.text.trim().isEmpty || _catalog.loading) return;
    _searchFocus.unfocus();
    _catalog.search(_query.text);
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: _catalog,
    builder: (context, _) {
      if (_catalog.error != null) {
        return FlowScaffold(
          eyebrow: 'CONNECTION ERROR',
          title: 'A small connection hiccup',
          backLabel: 'Search food',
          onBack: () {
            _catalog.changeQuery('');
            _query.clear();
          },
          actions: [
            AppButton(
              label: 'Try again',
              onPressed: () => _catalog.search(_query.text),
            ),
            AppButton(
              label: widget.selectIngredient
                  ? 'Back to recipe'
                  : 'Back to Today',
              variant: AppButtonVariant.secondary,
              onPressed: widget.selectIngredient
                  ? () => Navigator.pop(context)
                  : _today,
            ),
          ],
          children: [
            Text(
              widget.selectIngredient
                  ? 'Your recipe draft is safe.'
                  : 'Your diary is safe.',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const Text(
              'The food catalog couldn’t load. Check your connection and try again. Nothing new has been added.',
            ),
            const AppFeedbackCard(
              title: 'No changes were saved',
              detail: 'Your existing logs are unaffected.',
            ),
          ],
        );
      }
      final noResults = _catalog.searched && _catalog.foods.isEmpty;
      return FlowScaffold(
        eyebrow: noResults
            ? 'NO RESULTS'
            : widget.selectIngredient
            ? 'RECIPE BUILDER'
            : 'LOG FOOD',
        title: widget.selectIngredient ? 'Add an ingredient' : 'Find your food',
        actions: const [],
        backLabel: noResults
            ? 'Search food'
            : widget.selectIngredient
            ? 'Create recipe'
            : 'Today',
        onBack: () {
          if (noResults) {
            _query.clear();
            _catalog.changeQuery('');
          } else {
            Navigator.pop(context);
          }
        },
        children: [
          if (!noResults)
            Row(
              children: [
                Expanded(
                  child: AppButton(
                    label: '✓  Food name',
                    onPressed: () => _searchFocus.requestFocus(),
                    variant: AppButtonVariant.secondary,
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: AppButton(
                    label: 'Scan barcode',
                    onPressed: _scanning ? null : _scan,
                    variant: AppButtonVariant.secondary,
                  ),
                ),
              ],
            ),
          AppTextField(
            label: 'Search the food catalog',
            controller: _query,
            focusNode: _searchFocus,
            textInputAction: TextInputAction.search,
            onChanged: _catalog.changeQuery,
            onFieldSubmitted: (_) => _search(),
          ),
          AppButton(
            label: 'Search',
            onPressed: _catalog.loading || _query.text.trim().isEmpty
                ? null
                : _search,
          ),
          if (_catalog.loading) ...[
            const Center(
              child: Padding(
                padding: EdgeInsets.all(28),
                child: CircularProgressIndicator(),
              ),
            ),
            const Center(child: Text('Searching the food catalog…')),
          ] else if (noResults) ...[
            Text(
              'No matching foods',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const Text(
              'Try a simpler name, check the spelling or scan a package barcode instead.',
            ),
            AppButton(
              label: 'Clear search',
              variant: AppButtonVariant.secondary,
              onPressed: () {
                _query.clear();
                _catalog.changeQuery('');
              },
            ),
            AppButton(
              label: 'Scan a barcode',
              variant: AppButtonVariant.secondary,
              onPressed: _scan,
            ),
          ] else ...[
            if (_catalog.foods.isNotEmpty)
              Text(
                'BEST MATCHES',
                style: AppTheme.textStyle(11, color: AppColors.muted),
              ),
            for (final food in _catalog.foods)
              Material(
                color: AppColors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(18),
                  side: const BorderSide(color: AppColors.line),
                ),
                clipBehavior: Clip.antiAlias,
                child: InkWell(
                  onTap: () => _select(food),
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        SizedBox(
                          width: double.infinity,
                          child: Text(
                            '${food.name}${food.brand == null || food.brand!.isEmpty ? '' : ' · ${food.brand}'}',
                            style: AppTheme.textStyle(
                              15,
                              weight: FontWeight.w600,
                            ),
                          ),
                        ),
                        const SizedBox(height: 3),
                        Text(
                          '${amountTextFor(food.nutrition.nutrients.calories)} kcal / ${food.nutrition.basisLabel} · '
                          'P ${amountTextFor(food.nutrition.nutrients.protein)} · C ${amountTextFor(food.nutrition.nutrients.carbs)} · '
                          'F ${amountTextFor(food.nutrition.nutrients.fat)}',
                          style: AppTheme.textStyle(12, color: AppColors.muted),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            Text(
              'Search by food name or scan the barcode on a package.',
              style: AppTheme.textStyle(13, color: AppColors.muted),
            ),
          ],
          if (_catalog.notice != null && _catalog.notice!.isNotEmpty)
            Text(
              _catalog.notice!,
              style: AppTheme.textStyle(12, color: AppColors.muted),
            ),
        ],
      );
    },
  );
  String amountTextFor(double number) => number == number.roundToDouble()
      ? '${number.round()}'
      : number.toStringAsFixed(1);
}
