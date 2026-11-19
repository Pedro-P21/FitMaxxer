import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/catalog_food.dart';
import 'package:mobile/models/daily_log.dart';
import 'package:mobile/models/nutrition.dart';
import 'package:mobile/views/barcode_scanner_view.dart';

import 'logging_fixtures.dart';

void main() {
  test('grams and catalog servings scale from the nutrition basis', () {
    final food = CatalogFood.fromJson(yogurtFood);
    expect(food.nutrition.forQuantity(150, 'GRAM')!.calories, 150);
    expect(food.nutrition.forQuantity(2, 'SERVING')!.protein, 36);
    expect(food.nutrition.forQuantity(150, 'MILLILITER'), isNull);
    final item = LoggedItem.fromJson(yogurtItem(quantity: 200));
    expect(item.consumed!.calories, 200); // Snapshot's 100 kcal is per 100 g.
    expect(item.consumed!.protein, 24);
  });
  test('liquid and serving-based foods use their correct conversion', () {
    final liquid = CatalogFood.fromJson({
      ...yogurtFood,
      'nutrientBasis': 'PER_100_MILLILITERS',
      'servingQuantity': .25,
      'servingUnit': 'l',
      'caloriesKcal': 50,
    });
    expect(liquid.nutrition.units, ['MILLILITER', 'SERVING']);
    expect(liquid.nutrition.forQuantity(1, 'SERVING')!.calories, 125);
    expect(liquid.nutrition.forQuantity(1, 'GRAM'), isNull);
    final serving = CatalogFood.fromJson({
      ...yogurtFood,
      'nutrientBasis': 'PER_SERVING',
      'caloriesKcal': 200,
    });
    expect(serving.nutrition.forQuantity(75, 'GRAM')!.calories, 100);
    final unknownSize = CatalogFood.fromJson({
      ...yogurtFood,
      'servingUnit': 'bowl',
      'nutrientBasis': 'PER_SERVING',
    });
    expect(unknownSize.nutrition.units, ['SERVING']);
  });
  test(
    'unknown serving measurements cannot imply a mass or volume conversion',
    () {
      final food = CatalogFood.fromJson({...yogurtFood, 'servingUnit': 'bowl'});
      expect(food.nutrition.units, ['GRAM']);
      expect(food.nutrition.forQuantity(1, 'SERVING'), isNull);
    },
  );
  test(
    'portion validation rejects invalid, excessive and over-precise amounts',
    () {
      for (final text in [
        '0',
        '-1',
        'NaN',
        'Infinity',
        '1.00001',
        '1e3',
        '100000000',
      ]) {
        expect(quantityError(text), isNotNull, reason: text);
      }
      expect(quantityError('1.5', multiplier: 100), isNull);
      expect(quantityError('1000000', multiplier: 100), isNotNull);
      expect(quantityError('0,0001'), isNull);
    },
  );
  test(
    'barcode stays textual and only numeric product lengths are accepted',
    () {
      expect(validProductBarcode('05901234123457'), isTrue);
      for (final value in [
        'abc',
        'https://example.com',
        '123',
        '590 1234',
        '123456789012345',
      ]) {
        expect(validProductBarcode(value), isFalse);
      }
    },
  );
}
