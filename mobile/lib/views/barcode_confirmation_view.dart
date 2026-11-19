import 'package:material_ui/material_ui.dart';
import 'package:flutter/services.dart';

import '../models/catalog_food.dart';
import '../models/recipe.dart';
import 'ingredient_amount_view.dart';
import '../providers/daily_log_provider.dart';
import '../services/food_catalog_service.dart';
import '../theme/app_theme.dart';
import '../theme/app_colors.dart';
import '../widgets/app_button.dart';
import '../widgets/app_feedback_card.dart';
import '../widgets/app_text_field.dart';
import '../widgets/flow_scaffold.dart';
import 'barcode_scanner_view.dart';
import 'food_details_view.dart';

class BarcodeConfirmationView extends StatefulWidget {
  const BarcodeConfirmationView({
    super.key,
    required this.barcode,
    required this.catalog,
    required this.diary,
    required this.meal,
    this.selectIngredient = false,
  });
  final String barcode, meal;
  final bool selectIngredient;
  final FoodCatalogService catalog;
  final DailyLogProvider diary;
  @override
  State<BarcodeConfirmationView> createState() =>
      _BarcodeConfirmationViewState();
}

class _BarcodeConfirmationViewState extends State<BarcodeConfirmationView> {
  late final TextEditingController _code;
  final _form = GlobalKey<FormState>();
  bool _busy = false;
  String? _error;
  CatalogFood? _food;
  @override
  void initState() {
    super.initState();
    _code = TextEditingController(text: widget.barcode);
  }

  @override
  void dispose() {
    _code.dispose();
    super.dispose();
  }

  Future<void> _find() async {
    if (_busy || !_form.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final result = await widget.catalog.search(_code.text);
      if (!mounted) return;
      // Barcode lookup may return no product; never fall through to an unrelated result.
      _food = result.foods.where((food) {
        final barcode = food.barcode ?? food.id.split(':').last;
        return validProductBarcode(barcode) &&
            barcode.replaceFirst(RegExp(r'^0+'), '') ==
                _code.text.replaceFirst(RegExp(r'^0+'), '');
      }).firstOrNull;
      if (_food == null) {
        setState(() => _error = 'No matching food was found for this barcode.');
      } else {
        setState(() => _busy = false);
        final added = await Navigator.of(context).push<Object>(
          MaterialPageRoute(
            builder: (_) => widget.selectIngredient
                ? IngredientAmountView(food: _food!)
                : FoodDetailsView(
                    food: _food!,
                    diary: widget.diary,
                    meal: widget.meal,
                  ),
          ),
        );
        if (mounted && (added == true || added is DraftIngredient)) {
          Navigator.pop(context, added);
        }
      }
    } catch (_) {
      if (mounted) {
        setState(
          () => _error = 'The food catalog couldn’t load. Check your connection and try again.',
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Form(
    key: _form,
    child: FlowScaffold(
      eyebrow: widget.selectIngredient ? 'RECIPE BUILDER' : 'LOG FOOD',
      title: 'Confirm your barcode',
      backLabel: 'Search food',
      onBack: _busy ? null : () => Navigator.pop(context),
      actions: [
        AppButton(label: 'Find food', isLoading: _busy, onPressed: _find),
      ],
      children: [
        AppTextField(
          label: 'Barcode digits',
          controller: _code,
          enabled: !_busy,
          keyboardType: TextInputType.number,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          validator: (v) =>
              validProductBarcode(v) ? null : 'Use a barcode with 8–14 digits.',
        ),
        const Text('Check the number scanned from your product’s barcode.'),
        const AppFeedbackCard(
          title: 'Barcode scanned',
          detail: 'Confirm the digits, then find your food.',
        ),
        if (_error != null)
          Text(
            _error!,
            style: AppTheme.textStyle(13, color: AppColors.destructive),
          ),
      ],
    ),
  );
}
