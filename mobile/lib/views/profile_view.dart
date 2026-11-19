import 'package:material_ui/material_ui.dart';

import '../models/profile.dart';
import '../models/nutrition.dart';
import '../providers/session_provider.dart';
import '../services/api_client.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_bottom_navigation.dart';
import '../widgets/app_button.dart';
import '../widgets/nutrition_card.dart';
import 'profile_edit_view.dart';

class ProfileView extends StatefulWidget {
  const ProfileView({
    super.key,
    required this.session,
    required this.onToday,
    this.onHistory,
    this.onRecipes,
  });
  final SessionProvider session;
  final VoidCallback onToday;
  final VoidCallback? onHistory, onRecipes;
  @override
  State<ProfileView> createState() => _ProfileViewState();
}

class _ProfileViewState extends State<ProfileView> {
  UserProfile? _profile;
  bool _loading = true, _loggingOut = false;
  String? _error, _message;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final token = widget.session.api.token;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final profile = await widget.session.profiles.getProfile();
      if (!mounted || token != widget.session.api.token) return;
      if (profile == null) {
        throw const ApiException('No nutritional profile found.');
      }
      widget.session.acceptProfile(profile);
      setState(() => _profile = profile);
    } catch (e) {
      if (mounted) {
        setState(
          () => _error = e is ApiException
              ? e.message
              : 'Your profile could not be loaded. Please try again.',
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _edit(bool targets) async {
    final updated = await Navigator.of(context).push<UserProfile>(
      MaterialPageRoute(
        builder: (_) => ProfileEditView(
          session: widget.session,
          profile: _profile!,
          targets: targets,
        ),
      ),
    );
    if (!mounted || updated == null) return;
    setState(() {
      _profile = updated;
      _message = 'Your profile changes are saved';
    });
  }

  Future<void> _logout() async {
    setState(() {
      _loggingOut = true;
      _error = null;
    });
    try {
      await widget.session.logout();
    } catch (_) {
      if (mounted) {
        setState(
          () => _error = 'Could not clear your saved session. Please try logging out again.',
        );
      }
    } finally {
      if (mounted) setState(() => _loggingOut = false);
    }
  }

  Widget _card(List<Widget> children) => Container(
    width: double.infinity,
    padding: const EdgeInsets.all(18),
    decoration: BoxDecoration(
      color: AppColors.white,
      borderRadius: BorderRadius.circular(22),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: children,
    ),
  );

  @override
  Widget build(BuildContext context) => Scaffold(
    bottomNavigationBar: AppBottomNavigation(
      selected: 'Profile',
      onSelect: (tab) {
        if (_loggingOut) return;
        if (tab == 'Today') widget.onToday();
        if (tab == 'History') widget.onHistory?.call();
        if (tab == 'Recipes') widget.onRecipes?.call();
      },
    ),
    body: SafeArea(
      child: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(20),
          children: [
            Text(
              'KEEP IT PERSONAL',
              style: AppTheme.textStyle(
                10,
                color: AppColors.blue,
                weight: FontWeight.w600,
              ),
            ),
            Text(
              'Your profile',
              style: Theme.of(context).textTheme.headlineMedium,
            ),
            const SizedBox(height: 20),
            if (_loading) const Center(child: CircularProgressIndicator()),
            if (_error != null) ...[
              InlineNotice(_error!, error: true),
              if (!_loggingOut) AppButton(label: 'Try again', onPressed: _load),
              const SizedBox(height: 12),
            ],
            if (_message != null) ...[
              InlineNotice(_message!, success: true),
              const SizedBox(height: 14),
            ],
            if (_profile case final p?) ...[
              if (p.userId != null) ...[
                Text(
                  '@${p.userId}',
                  style: AppTheme.textStyle(20, weight: FontWeight.w700),
                ),
                const SizedBox(height: 16),
              ],
              _card([
                Text(
                  'Your baseline',
                  style: AppTheme.textStyle(16, weight: FontWeight.w700),
                ),
                const SizedBox(height: 10),
                Text(
                  '${p.age} years · ${p.gender == 'MALE' ? 'Male' : 'Female'}',
                ),
                Text(
                  '${amountText(p.weightKg)} kg · ${amountText(p.heightCm)} cm',
                ),
                Text(
                  p.bodyFat == null
                      ? 'Body fat: not provided'
                      : 'Body fat: ${amountText(p.bodyFat!)}%',
                ),
                Text(
                  '${activityLabels[p.activity] ?? p.activity} · ${p.goal?.label ?? 'Custom targets'}',
                ),
                const Text('Units: metric'),
                Text('Maintenance calories: ${p.maintenance} kcal / day'),
                TextButton(
                  onPressed: _loading || _loggingOut
                      ? null
                      : () => _edit(false),
                  child: const Text('Edit physical data →'),
                ),
              ]),
              const SizedBox(height: 14),
              _card([
                Text(
                  '${p.calories} kcal / day',
                  style: AppTheme.textStyle(
                    26,
                    color: AppColors.blue,
                    weight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  'Protein ${p.protein} g · Carbs ${p.carbs} g · Fat ${p.fat} g',
                  style: AppTheme.textStyle(12, color: AppColors.muted),
                ),
                TextButton(
                  onPressed: _loading || _loggingOut ? null : () => _edit(true),
                  child: const Text('Edit daily targets →'),
                ),
              ]),
              const SizedBox(height: 14),
            ],
            AppButton(
              label: 'Log out',
              variant: AppButtonVariant.secondary,
              isLoading: _loggingOut,
              onPressed: _logout,
            ),
          ],
        ),
      ),
    ),
  );
}
