import 'package:material_ui/material_ui.dart';

import '../models/goal_option.dart';
import '../models/profile.dart';
import '../providers/session_provider.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_button.dart';
import '../widgets/app_feedback_card.dart';
import '../widgets/app_select_field.dart';
import '../widgets/app_text_field.dart';
import '../widgets/flow_scaffold.dart';

class OnboardingView extends StatefulWidget {
  const OnboardingView({super.key, required this.session});
  final SessionProvider session;
  @override
  State<OnboardingView> createState() => _OnboardingViewState();
}

class _OnboardingViewState extends State<OnboardingView> {
  final _form = GlobalKey<FormState>();
  late final TextEditingController _age, _weight, _height, _bodyFat;
  final _inches = TextEditingController();
  int _step = 0;
  bool _imperial = false;
  ProfileDraft get draft => widget.session.draft;
  @override
  void initState() {
    super.initState();
    _age = TextEditingController(text: draft.age);
    _weight = TextEditingController(text: draft.weightKg);
    _height = TextEditingController(text: draft.heightCm);
    _bodyFat = TextEditingController(text: draft.bodyFat);
  }

  @override
  void dispose() {
    for (final c in [_age, _weight, _height, _bodyFat, _inches]) {
      c.dispose();
    }
    super.dispose();
  }

  double? number(String? value) =>
      double.tryParse((value ?? '').trim().replaceAll(',', '.'));
  double? get kg {
    final v = number(_weight.text);
    return v == null
        ? null
        : _imperial
        ? v / 2.2046226218
        : v;
  }

  double? get cm {
    final h = number(_height.text);
    if (!_imperial) return h;
    final inches = number(_inches.text);
    return h == null || inches == null ? null : (h * 12 + inches) * 2.54;
  }

  String short(double value) =>
      value.toStringAsFixed(4).replaceFirst(RegExp(r'\.?0+$'), '');
  void _changeUnits(bool imperial) {
    if (imperial == _imperial) return;
    final weight = kg, height = cm;
    setState(() {
      _imperial = imperial;
      if (weight != null && weight.isFinite) {
        _weight.text = short(imperial ? weight * 2.2046226218 : weight);
      }
      if (height != null && height.isFinite) {
        if (imperial) {
          final total = height / 2.54;
          final feet = (total / 12).floor();
          _height.text = '$feet';
          _inches.text = short(total - feet * 12);
        } else {
          _height.text = short(height);
        }
      } else if (imperial && _height.text.isEmpty) {
        _inches.text = '0';
      }
    });
  }

  void _next() {
    FocusScope.of(context).unfocus();
    if (!_form.currentState!.validate()) return;
    draft.age = _age.text.trim();
    draft.weightKg = kg!.toString();
    draft.heightCm = cm!.toString();
    draft.bodyFat = _bodyFat.text.trim().isEmpty
        ? ''
        : number(_bodyFat.text).toString();
    setState(() => _step = 1);
  }

  Future<void> _calculate() async {
    if (await widget.session.saveProfile() && mounted) {
      setState(() => _step = 2);
    }
  }

  @override
  Widget build(BuildContext context) {
    final busy = widget.session.busy;
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) {
        if (!didPop && _step > 0 && !busy) setState(() => _step--);
      },
      child: FlowScaffold(
        eyebrow: [
          '01 / 03  •  YOUR BASELINE',
          '02 / 03  •  ACTIVITY & GOAL',
          '03 / 03  •  MADE FOR YOU',
        ][_step],
        title: [
          'Let’s start with you',
          'Find your balance',
          'Your daily starting point',
        ][_step],
        onBack: _step == 0 || busy ? null : () => setState(() => _step--),
        actions: _step == 2
            ? [
                AppButton(
                  label: 'Start my day',
                  onPressed: widget.session.finishOnboarding,
                ),
                AppButton(
                  label: 'Adjust targets',
                  variant: AppButtonVariant.secondary,
                  onPressed: () => setState(() => _step = 1),
                ),
              ]
            : [
                AppButton(
                  label: _step == 0 ? 'Continue' : 'Calculate my targets',
                  isLoading: busy,
                  onPressed: _step == 0 ? _next : _calculate,
                ),
              ],
        children: [
          if (_step == 0)
            Form(
              key: _form,
              child: Column(
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: AppButton(
                          label: '${_imperial ? '' : '✓  '}Metric',
                          variant: AppButtonVariant.secondary,
                          onPressed: () => _changeUnits(false),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: AppButton(
                          label: '${_imperial ? '✓  ' : ''}Imperial',
                          variant: AppButtonVariant.secondary,
                          onPressed: () => _changeUnits(true),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: AppTextField(
                          label: 'Age',
                          controller: _age,
                          keyboardType: TextInputType.number,
                          textInputAction: TextInputAction.next,
                          validator: (v) {
                            final n = int.tryParse(v?.trim() ?? '');
                            return n == null || n < 1 || n > 120
                                ? 'Enter an age from 1 to 120.'
                                : null;
                          },
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: AppSelectField(
                          label: 'Gender',
                          value: draft.gender,
                          options: const {'MALE': 'Male', 'FEMALE': 'Female'},
                          onChanged: (v) => setState(() => draft.gender = v),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),
                  AppTextField(
                    label: _imperial ? 'Weight · lb' : 'Weight · kg',
                    controller: _weight,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    textInputAction: TextInputAction.next,
                    validator: (_) =>
                        kg == null || !kg!.isFinite || kg! <= 10 || kg! > 635
                        ? 'Enter a weight above 10 kg and up to 635 kg (${_imperial ? 'in pounds' : 'in kilograms'}).'
                        : null,
                  ),
                  const SizedBox(height: 14),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: AppTextField(
                          label: _imperial ? 'Height · feet' : 'Height · cm',
                          controller: _height,
                          keyboardType: const TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                          textInputAction: TextInputAction.next,
                          validator: (_) =>
                              cm == null ||
                                  !cm!.isFinite ||
                                  cm! <= 100 ||
                                  cm! > 250
                              ? 'Enter a height above 100 cm and up to 250 cm.'
                              : null,
                        ),
                      ),
                      if (_imperial) ...[
                        const SizedBox(width: 10),
                        Expanded(
                          child: AppTextField(
                            label: 'Height · inches',
                            controller: _inches,
                            keyboardType: const TextInputType.numberWithOptions(
                              decimal: true,
                            ),
                            textInputAction: TextInputAction.next,
                            validator: (v) {
                              final n = number(v);
                              return n == null ||
                                      !n.isFinite ||
                                      n < 0 ||
                                      n >= 12
                                  ? 'Enter inches from 0 to below 12.'
                                  : null;
                            },
                          ),
                        ),
                      ],
                    ],
                  ),
                  const SizedBox(height: 14),
                  AppTextField(
                    label: 'Body fat · optional (%)',
                    hintText: 'Not provided',
                    controller: _bodyFat,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    textInputAction: TextInputAction.done,
                    validator: (v) {
                      if (v == null || v.trim().isEmpty) return null;
                      final n = number(v);
                      return n == null || !n.isFinite || n < 3 || n > 80
                          ? 'Enter a body fat percentage from 3 to 80.'
                          : null;
                    },
                  ),
                  const SizedBox(height: 14),
                  Text(
                    'These details help estimate your energy needs. You can change them later.',
                    style: AppTheme.textStyle(
                      12,
                      color: AppColors.muted,
                      height: 1.35,
                    ),
                  ),
                ],
              ),
            ),
          if (_step == 1) ...[
            Text(
              'How active is your typical week?',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            AppSelectField(
              label: 'Activity level',
              value: draft.activity,
              options: activityLabels,
              onChanged: busy
                  ? null
                  : (v) => setState(() => draft.activity = v),
            ),
            Text(
              activityDescriptions[draft.activity]!,
              style: Theme.of(context).textTheme.bodySmall,
            ),
            Text(
              'What are you working toward?',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            for (final goal in GoalOption.values) _goalCard(goal, busy),
            Text(
              'Targets are estimates, not medical advice.',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
          if (_step == 2) ..._targets(context),
          if (widget.session.error != null)
            AppFeedbackCard(
              title: 'Unable to continue',
              detail: widget.session.error!,
            ),
        ],
      ),
    );
  }

  Widget _goalCard(GoalOption goal, bool busy) {
    final selected = draft.goal == goal;
    return Semantics(
      selected: selected,
      inMutuallyExclusiveGroup: true,
      child: Material(
        color: selected ? AppColors.pale : AppColors.white,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(22),
          side: BorderSide(color: selected ? AppColors.blue : AppColors.line),
        ),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: busy ? null : () => setState(() => draft.goal = goal),
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        goal.label,
                        style: AppTheme.textStyle(
                          15,
                          weight: FontWeight.w600,
                          height: 1.35,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        goal.description,
                        style: AppTheme.textStyle(
                          12,
                          color: AppColors.muted,
                          height: 1.35,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 10),
                Icon(
                  selected
                      ? Icons.radio_button_checked
                      : Icons.radio_button_unchecked,
                  size: 24,
                  color: AppColors.blue,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  List<Widget> _targets(BuildContext context) {
    final p = widget.session.profile!;
    final offset = p.calories - p.maintenance;
    return [
      Container(
        width: double.infinity,
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(32),
          gradient: AppTheme.targetGradient,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '${draft.goal.label.toUpperCase()} TARGET',
              style: AppTheme.textStyle(
                11,
                weight: FontWeight.w600,
                color: AppColors.white,
              ),
            ),
            Text(
              '${p.calories}',
              style: AppTheme.textStyle(
                52,
                weight: FontWeight.w700,
                color: AppColors.white,
                height: 1.35,
              ),
            ),
            Text(
              'calories per day',
              style: AppTheme.textStyle(
                15,
                weight: FontWeight.w500,
                color: AppColors.white,
              ),
            ),
          ],
        ),
      ),
      Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (final entry in {
            'Protein': p.protein,
            'Carbs': p.carbs,
            'Fat': p.fat,
          }.entries) ...[
            if (entry.key != 'Protein') const SizedBox(width: 10),
            Expanded(
              child: Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.white,
                  borderRadius: BorderRadius.circular(22),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${entry.value} g',
                      style: AppTheme.textStyle(
                        21,
                        weight: FontWeight.w700,
                        color: AppColors.blue,
                      ),
                    ),
                    Text(
                      entry.key,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
            ),
          ],
        ],
      ),
      AppFeedbackCard(
        title: 'Calculated from your baseline',
        detail:
            '${p.weightKg.toStringAsFixed(1)} kg · ${p.heightCm.toStringAsFixed(1)} cm · age ${p.age} · ${activityLabels[p.activity]!.toLowerCase()}',
      ),
      Text(
        'Estimated maintenance: ${p.maintenance} kcal\n'
        'Goal adjustment: ${offset > 0 ? '+' : ''}$offset kcal\n'
        'Method: Mifflin–St Jeor',
        style: AppTheme.textStyle(13, color: AppColors.muted, height: 1.35),
      ),
    ];
  }
}
