import 'dart:async';

import 'package:material_ui/material_ui.dart';

import '../models/daily_log.dart';
import '../providers/daily_log_provider.dart';
import '../providers/session_provider.dart';
import '../services/daily_log_service.dart';
import '../services/food_catalog_service.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_button.dart';
import '../widgets/app_bottom_navigation.dart';
import '../widgets/app_feedback_card.dart';
import '../widgets/nutrition_card.dart';
import 'food_search_view.dart';
import 'profile_view.dart';

class TodayView extends StatefulWidget {
  const TodayView({
    super.key,
    required this.session,
    this.diary,
    this.scanBarcode,
  });
  final SessionProvider session;
  final DailyLogProvider? diary;
  final BarcodeScan? scanBarcode;
  @override
  State<TodayView> createState() => _TodayViewState();
}

class _TodayViewState extends State<TodayView> with WidgetsBindingObserver {
  late final DailyLogProvider _diary;
  final Set<String> _expandedMeals = {};
  bool _showProfile = false;
  @override
  void initState() {
    super.initState();
    _diary =
        widget.diary ?? DailyLogProvider(DailyLogService(widget.session.api));
    WidgetsBinding.instance.addObserver(this);
    unawaited(_diary.load());
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed &&
        widget.session.api.token != null) {
      unawaited(_diary.load());
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    if (widget.diary == null) _diary.dispose();
    super.dispose();
  }

  Future<void> _search([String meal = 'Breakfast']) async {
    _diary.clearFeedback();
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => FoodSearchView(
          catalog: FoodCatalogService(widget.session.api),
          diary: _diary,
          meal: meal,
          scanBarcode: widget.scanBarcode,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: _diary,
    builder: (context, _) => _showProfile
        ? ProfileView(
            session: widget.session,
            onToday: () {
              setState(() => _showProfile = false);
              unawaited(_diary.load());
            },
          )
        : Scaffold(
            bottomNavigationBar: _navigation(),
            body: SafeArea(
              bottom: false,
              child: RefreshIndicator(
                onRefresh: _diary.load,
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(20, 10, 20, 24),
                  physics: const AlwaysScrollableScrollPhysics(),
                  children: [
                    Text(
                      _dateLabel(_diary.log?.date ?? DateTime.now()),
                      style: AppTheme.textStyle(
                        10,
                        weight: FontWeight.w600,
                        color: AppColors.blue,
                      ),
                    ),
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            'Today',
                            style: Theme.of(context).textTheme.headlineMedium,
                          ),
                        ),
                        TextButton.icon(
                          onPressed: _diary.log == null || _diary.busy
                              ? null
                              : _search,
                          icon: const Icon(Icons.add, size: 18),
                          label: const Text('Log food'),
                        ),
                      ],
                    ),
                    if (_diary.log == null && _diary.loading) ...[
                      Container(
                        padding: const EdgeInsets.all(28),
                        decoration: BoxDecoration(
                          color: AppColors.white,
                          borderRadius: BorderRadius.circular(24),
                        ),
                        child: Column(
                          children: [
                            const CircularProgressIndicator(),
                            const SizedBox(height: 20),
                            Text(
                              'Syncing your meals…',
                              style: Theme.of(context).textTheme.headlineSmall,
                            ),
                            const SizedBox(height: 10),
                            const Text(
                              'Fetching your latest logs and daily targets.',
                            ),
                          ],
                        ),
                      ),
                    ],
                    if (_diary.error != null) ...[
                      AppFeedbackCard(
                        title: 'Unable to load or update your diary',
                        detail: _diary.error!,
                      ),
                      const SizedBox(height: 10),
                      AppButton(
                        label: 'Refresh diary',
                        onPressed: _diary.busy ? null : _diary.load,
                      ),
                      const SizedBox(height: 10),
                    ],
                    if (_diary.log != null) _content(_diary.log!),
                  ],
                ),
              ),
            ),
          ),
  );
  Widget _content(DailyLog log) {
    final count = log.meals.fold<int>(
      0,
      (sum, meal) => sum + meal.items.length,
    );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (_diary.loading) const LinearProgressIndicator(),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(36),
            gradient: AppTheme.todayGradient,
          ),
          child: Center(
            child: SizedBox.square(
              dimension: 202,
              child: Stack(
                children: [
                  Positioned(
                    left: 4,
                    top: 4,
                    width: 194,
                    height: 194,
                    child: CircularProgressIndicator(
                      key: const ValueKey('calorie-progress'),
                      strokeWidth: 6,
                      backgroundColor: AppColors.white.withValues(alpha: .2),
                      color: AppColors.macroCarbs,
                      value: log.targets.calories <= 0
                          ? 0
                          : (log.consumed.calories / log.targets.calories)
                                .clamp(0, 1),
                    ),
                  ),
                  if (log.targets.calories > 0 &&
                      log.consumed.calories > log.targets.calories)
                    Positioned(
                      left: 4,
                      top: 4,
                      width: 194,
                      height: 194,
                      child: CircularProgressIndicator(
                        key: const ValueKey('calorie-overflow'),
                        strokeWidth: 6,
                        color: AppColors.macroFat,
                        value:
                            ((log.consumed.calories - log.targets.calories) /
                                    log.targets.calories)
                                .clamp(0, 1),
                      ),
                    ),
                  Center(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 12),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          FittedBox(
                            child: Text(
                              '${log.consumed.calories.round()}',
                              style: AppTheme.textStyle(
                                42,
                                weight: FontWeight.w700,
                                color: AppColors.white,
                                height: 1.2,
                              ),
                            ),
                          ),
                          Text(
                            'of ${log.targets.calories.round()} kcal consumed',
                            textAlign: TextAlign.center,
                            style: AppTheme.textStyle(
                              11,
                              weight: FontWeight.w500,
                              color: AppColors.white,
                            ),
                          ),
                          const SizedBox(height: 20),
                          Text(
                            '${log.remaining.calories.abs().round()} ${log.remaining.calories < 0 ? 'over target' : 'kcal remaining'}',
                            textAlign: TextAlign.center,
                            style: AppTheme.textStyle(
                              12,
                              weight: FontWeight.w600,
                              color: AppColors.white,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        const SizedBox(height: 10),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: _macro(
                'Protein',
                log.consumed.protein,
                log.targets.protein,
                log.remaining.protein,
                AppColors.macroProtein,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _macro(
                'Carbs',
                log.consumed.carbs,
                log.targets.carbs,
                log.remaining.carbs,
                AppColors.macroCarbs,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _macro(
                'Fat',
                log.consumed.fat,
                log.targets.fat,
                log.remaining.fat,
                AppColors.macroFat,
              ),
            ),
          ],
        ),
        if (_diary.message != null || log.remaining.calories < 0) ...[
          const SizedBox(height: 10),
          InlineNotice(
            success: _diary.message != null,
            warning: _diary.message == null && log.remaining.calories < 0,
            _diary.message ??
                '${log.remaining.calories.abs().round()} kcal over · a fresh start tomorrow',
          ),
        ],
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(
              child: Text(
                'Your meals',
                style: AppTheme.textStyle(16, weight: FontWeight.w700),
              ),
            ),
            Text(
              count == 0 ? '0 items' : 'Meal diary',
              style: AppTheme.textStyle(11, color: AppColors.muted),
            ),
          ],
        ),
        const SizedBox(height: 10),
        Material(
          color: AppColors.white,
          borderRadius: BorderRadius.circular(22),
          clipBehavior: Clip.antiAlias,
          child: Column(
            children: [for (final type in mealTypes) _meal(log.meal(type))],
          ),
        ),
      ],
    );
  }

  Widget _macro(
    String name,
    double consumed,
    double target,
    double remaining,
    Color progressColor,
  ) => Container(
    padding: const EdgeInsets.all(10),
    decoration: BoxDecoration(
      color: AppColors.white,
      borderRadius: BorderRadius.circular(18),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(name, style: AppTheme.textStyle(11, color: AppColors.muted)),
        Text(
          '${consumed.round()} / ${target.round()} g',
          style: AppTheme.textStyle(13, weight: FontWeight.w700),
        ),
        Text(
          '${remaining.abs().round()} g ${remaining < 0 ? 'over' : 'left'}',
          style: AppTheme.textStyle(
            10,
            color: remaining < 0 ? AppColors.destructive : AppColors.blue,
          ),
        ),
        const SizedBox(height: 3),
        LinearProgressIndicator(
          minHeight: 3,
          color: progressColor,
          backgroundColor: AppColors.pale,
          value: target <= 0 ? 0 : (consumed / target).clamp(0, 1),
        ),
      ],
    ),
  );
  Widget _meal(LoggedMeal meal) {
    final expanded = _expandedMeals.contains(meal.type);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Dismissible(
            key: ValueKey('swipe-meal-${meal.type}'),
            direction: _diary.busy
                ? DismissDirection.none
                : DismissDirection.endToStart,
            dismissThresholds: const {DismissDirection.endToStart: .4},
            background: Container(
              color: AppColors.success,
              alignment: Alignment.centerRight,
              padding: const EdgeInsets.only(right: 20),
              child: const Icon(Icons.add, color: AppColors.white, size: 28),
            ),
            confirmDismiss: (_) async {
              if (!_diary.busy) await _search(meal.type);
              return false;
            },
            child: ColoredBox(
              color: AppColors.white,
              child: Row(
                key: ValueKey('meal-header-${meal.type}'),
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          meal.type,
                          style: AppTheme.textStyle(
                            13,
                            weight: FontWeight.w600,
                          ),
                        ),
                        if (meal.items.isEmpty)
                          Text(
                            'Nothing logged yet',
                            style: AppTheme.textStyle(
                              10,
                              color: AppColors.muted,
                            ),
                          )
                        else if (meal.items.length == 1)
                          InkWell(
                            key: ValueKey(
                              'logged-item-${meal.items.single.id}',
                            ),
                            onTap: null,
                            child: Padding(
                              padding: const EdgeInsets.symmetric(vertical: 6),
                              child: Text(
                                meal.items.single.name,
                                style: AppTheme.textStyle(
                                  10,
                                  color: AppColors.muted,
                                ),
                              ),
                            ),
                          )
                        else
                          Semantics(
                            expanded: expanded,
                            child: TextButton(
                              key: ValueKey('toggle-meal-${meal.type}'),
                              style: TextButton.styleFrom(
                                padding: EdgeInsets.zero,
                                alignment: Alignment.centerLeft,
                              ),
                              onPressed: () => setState(() {
                                if (expanded) {
                                  _expandedMeals.remove(meal.type);
                                } else {
                                  _expandedMeals.add(meal.type);
                                }
                              }),
                              child: Text(
                                'Show logged items',
                                style: AppTheme.textStyle(
                                  10,
                                  color: AppColors.muted,
                                ),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                  Tooltip(
                    message: 'Add food to ${meal.type}',
                    child: TextButton(
                      onPressed: _diary.busy ? null : () => _search(meal.type),
                      child: meal.items.isEmpty
                          ? const Icon(
                              Icons.add,
                              size: 18,
                              color: AppColors.blue,
                            )
                          : Text(
                              '${meal.consumed.calories.round()}',
                              key: ValueKey('meal-total-${meal.type}'),
                              style: AppTheme.textStyle(
                                13,
                                weight: FontWeight.w600,
                                color: AppColors.blue,
                              ),
                            ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          if (meal.items.length > 1 && expanded)
            for (final item in meal.items)
              InkWell(
                key: ValueKey('logged-item-${item.id}'),
                onTap: null,
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 10),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Text(
                          '${item.name} · ${item.portion}',
                          style: AppTheme.textStyle(10, color: AppColors.muted),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Text(
                        '${item.consumed?.calories.round() ?? '—'}',
                        style: AppTheme.textStyle(
                          13,
                          weight: FontWeight.w600,
                          color: AppColors.blue,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
        ],
      ),
    );
  }

  Widget _navigation() => AppBottomNavigation(
    selected: 'Today',
    onSelect: (tab) {
      if (tab == 'Profile') setState(() => _showProfile = true);
    },
  );
  String _dateLabel(DateTime date) {
    const days = [
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
      'SUNDAY',
    ];
    const months = [
      'JANUARY',
      'FEBRUARY',
      'MARCH',
      'APRIL',
      'MAY',
      'JUNE',
      'JULY',
      'AUGUST',
      'SEPTEMBER',
      'OCTOBER',
      'NOVEMBER',
      'DECEMBER',
    ];
    return '${days[date.weekday - 1]}, ${months[date.month - 1]} ${date.day}';
  }
}
