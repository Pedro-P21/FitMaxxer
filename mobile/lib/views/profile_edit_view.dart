import 'package:material_ui/material_ui.dart';

import '../models/goal_option.dart';
import '../models/nutrition.dart';
import '../models/profile.dart';
import '../providers/session_provider.dart';
import '../services/api_client.dart';
import '../widgets/app_button.dart';
import '../widgets/app_select_field.dart';
import '../widgets/app_text_field.dart';
import '../widgets/flow_scaffold.dart';
import '../widgets/nutrition_card.dart';

class ProfileEditView extends StatefulWidget {
  const ProfileEditView({
    super.key,
    required this.session,
    required this.profile,
    required this.targets,
  });
  final SessionProvider session;
  final UserProfile profile;
  final bool targets;
  @override
  State<ProfileEditView> createState() => _ProfileEditViewState();
}

class _ProfileEditViewState extends State<ProfileEditView> {
  final _form = GlobalKey<FormState>();
  late final Map<String, TextEditingController> _fields;
  late String _gender, _activity, _goal;
  bool _busy = false, _manualMacros = false;
  String? _error;
  @override
  void initState() {
    super.initState();
    final p = widget.profile;
    _gender = p.gender;
    _activity = p.activity;
    _goal = p.goal?.apiValue ?? 'CUSTOM';
    _fields = {
      'age': TextEditingController(text: '${p.age}'),
      'weightKg': TextEditingController(text: amountText(p.weightKg)),
      'heightCm': TextEditingController(text: amountText(p.heightCm)),
      'bodyFatPercentage': TextEditingController(
        text: p.bodyFat == null ? '' : amountText(p.bodyFat!),
      ),
      'calorieTarget': TextEditingController(text: '${p.calories}'),
      'proteinGrams': TextEditingController(text: '${p.protein}'),
      'carbsGrams': TextEditingController(text: '${p.carbs}'),
      'fatsGrams': TextEditingController(text: '${p.fat}'),
    };
  }

  @override
  void dispose() {
    for (final field in _fields.values) {
      field.dispose();
    }
    super.dispose();
  }

  double? _number(String key) =>
      double.tryParse(_fields[key]!.text.trim().replaceAll(',', '.'));
  Widget _field(
    String key,
    String label, {
    required double min,
    required double max,
    bool integer = false,
    bool exclusiveMin = false,
    bool optional = false,
  }) => AppTextField(
    label: label,
    controller: _fields[key],
    keyboardType: TextInputType.numberWithOptions(decimal: !integer),
    textInputAction: TextInputAction.next,
    validator: (_) {
      if (optional && _fields[key]!.text.trim().isEmpty) {
        return widget.profile.bodyFat == null
            ? null
            : 'Enter a value; an existing body fat value cannot be cleared.';
      }
      final n = _number(key);
      if (n == null ||
          !n.isFinite ||
          n < min ||
          n > max ||
          exclusiveMin && n == min ||
          integer && n != n.roundToDouble()) {
        return 'Enter ${integer ? 'a whole number' : 'a value'} ${exclusiveMin ? 'above' : 'from'} ${amountText(min)} to ${amountText(max)}.';
      }
      return null;
    },
  );

  Future<void> _save() async {
    if (_busy || !_form.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    final changes = <String, dynamic>{};
    if (widget.targets) {
      changes['calorieTarget'] = _number('calorieTarget')!.toInt();
      if (_manualMacros) {
        final macros = {
          for (final key in ['proteinGrams', 'carbsGrams', 'fatsGrams'])
            key: _number(key)!.toInt(),
        };
        final calories =
            macros['proteinGrams']! * 4 +
            macros['carbsGrams']! * 4 +
            macros['fatsGrams']! * 9;
        if ((calories - (changes['calorieTarget'] as int)).abs() > 15) {
          setState(
            () => _error = 'Calories from macros must be within 15 kcal of your daily target.',
          );
          return;
        }
        changes['macroBreakdown'] = macros;
      }
    } else {
      changes.addAll({
        'age': _number('age')!.toInt(),
        'weightKg': _number('weightKg'),
        'heightCm': _number('heightCm'),
        'gender': _gender,
        'activityLevel': _activity,
        'unitSystem': 'METRIC',
        if (_fields['bodyFatPercentage']!.text.trim().isNotEmpty)
          'bodyFatPercentage': _number('bodyFatPercentage'),
        if (_goal != 'CUSTOM') 'goalType': _goal,
        if (_goal == 'CUSTOM') 'calorieTarget': widget.profile.calories,
      });
    }
    final token = widget.session.api.token;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final updated = await widget.session.profiles.update(
        widget.profile,
        changes,
      );
      if (!mounted ||
          token != widget.session.api.token ||
          widget.session.stage != SessionStage.ready) {
        return;
      }
      widget.session.acceptProfile(updated);
      Navigator.pop(context, updated);
    } catch (e) {
      if (mounted) {
        setState(
          () => _error = e is ApiException
              ? e.message
              : 'Your profile could not be saved. Please try again.',
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => PopScope(
    canPop: !_busy,
    child: FlowScaffold(
      eyebrow: 'YOUR PROFILE',
      title: widget.targets ? 'Edit daily targets' : 'Edit physical data',
      backLabel: 'Your profile',
      onBack: _busy ? null : () => Navigator.pop(context),
      actions: [
        AppButton(label: 'Save changes', isLoading: _busy, onPressed: _save),
      ],
      children: [
        Form(
          key: _form,
          child: AbsorbPointer(
            absorbing: _busy,
            child: Column(
              children: [
                if (widget.targets) ...[
                  _field(
                    'calorieTarget',
                    'Daily calories · kcal',
                    min: 500,
                    max: 2147483647,
                    integer: true,
                  ),
                  SwitchListTile(
                    title: const Text('Set macros manually'),
                    value: _manualMacros,
                    onChanged: (v) => setState(() => _manualMacros = v),
                  ),
                  if (_manualMacros) ...[
                    _field(
                      'proteinGrams',
                      'Protein · g',
                      min: 0,
                      max: 2147483647,
                      integer: true,
                    ),
                    const SizedBox(height: 14),
                    _field(
                      'carbsGrams',
                      'Carbs · g',
                      min: 0,
                      max: 2147483647,
                      integer: true,
                    ),
                    const SizedBox(height: 14),
                    _field(
                      'fatsGrams',
                      'Fat · g',
                      min: 0,
                      max: 2147483647,
                      integer: true,
                    ),
                    const SizedBox(height: 14),
                    const Text(
                      'Protein and carbs provide 4 kcal/g; fat provides 9 kcal/g. Their total must match your target within 15 kcal.',
                    ),
                  ] else
                    const Text(
                      'Changing calories recalculates your macros automatically. If calories stay the same, your current macros are kept.',
                    ),
                ] else ...[
                  _field('age', 'Age', min: 1, max: 120, integer: true),
                  const SizedBox(height: 14),
                  AppSelectField(
                    label: 'Gender',
                    value: _gender,
                    options: const {'MALE': 'Male', 'FEMALE': 'Female'},
                    onChanged: (v) => setState(() => _gender = v),
                  ),
                  const SizedBox(height: 14),
                  _field(
                    'weightKg',
                    'Weight · kg',
                    min: 10,
                    max: 635,
                    exclusiveMin: true,
                  ),
                  const SizedBox(height: 14),
                  _field(
                    'heightCm',
                    'Height · cm',
                    min: 100,
                    max: 250,
                    exclusiveMin: true,
                  ),
                  const SizedBox(height: 14),
                  _field(
                    'bodyFatPercentage',
                    'Body fat · optional (%)',
                    min: 3,
                    max: 80,
                    optional: true,
                  ),
                  if (widget.profile.bodyFat != null)
                    const Text(
                      'You can update body fat; removing a saved value is not currently supported.',
                    ),
                  const SizedBox(height: 14),
                  AppSelectField(
                    label: 'Activity level',
                    value: _activity,
                    options: activityLabels,
                    onChanged: (v) => setState(() => _activity = v),
                  ),
                  const SizedBox(height: 14),
                  AppSelectField(
                    label: 'Goal',
                    value: _goal,
                    options: {
                      for (final g in GoalOption.values) g.apiValue: g.label,
                      if (widget.profile.goal == null)
                        'CUSTOM': 'Keep custom calorie target',
                    },
                    onChanged: (v) => setState(() => _goal = v),
                  ),
                  const SizedBox(height: 14),
                  const Text(
                    'Saving recalculates your daily targets using these measurements, activity level and goal. Custom macros are replaced by the calculated split.',
                  ),
                ],
              ],
            ),
          ),
        ),
        if (_error != null) InlineNotice(_error!, error: true),
      ],
    ),
  );
}
