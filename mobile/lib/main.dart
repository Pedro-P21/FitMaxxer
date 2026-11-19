import 'dart:async';

import 'package:material_ui/material_ui.dart';

import 'providers/session_provider.dart';
import 'services/api_client.dart';
import 'services/token_store.dart';
import 'theme/app_theme.dart';
import 'views/auth_view.dart';
import 'views/onboarding_view.dart';
import 'views/session_expired_view.dart';
import 'views/welcome_view.dart';
import 'widgets/app_button.dart';
import 'widgets/app_feedback_card.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key, this.session});
  final SessionProvider? session;
  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> with WidgetsBindingObserver {
  late final SessionProvider _session;
  String _signedOutScreen = 'welcome';
  final _navigator = GlobalKey<NavigatorState>();
  SessionStage? _previousStage;

  void _sessionChanged() {
    final leavingDiary =
        _previousStage == SessionStage.ready &&
        _session.stage != SessionStage.ready;
    _previousStage = _session.stage;
    if (leavingDiary) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          _navigator.currentState?.popUntil((route) => route.isFirst);
        }
      });
    }
  }

  @override
  void initState() {
    super.initState();
    _session =
        widget.session ??
        SessionProvider(api: ApiClient(), store: SecureTokenStore());
    _session.addListener(_sessionChanged);
    WidgetsBinding.instance.addObserver(this);
    unawaited(_session.restore());
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _session.checkExpiry();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _session.removeListener(_sessionChanged);
    if (widget.session == null) {
      _session.dispose();
      _session.api.close();
    }
    super.dispose();
  }

  void _show(String screen, {bool preserveError = false}) {
    if (!preserveError) _session.clearError();
    setState(() => _signedOutScreen = screen);
  }

  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'FitMaxxer',
    navigatorKey: _navigator,
    debugShowCheckedModeBanner: false,
    theme: AppTheme.light,
    home: ListenableBuilder(
      listenable: _session,
      builder: (context, _) => switch (_session.stage) {
        SessionStage.starting || SessionStage.checkingProfile => const Scaffold(
          body: Center(child: CircularProgressIndicator()),
        ),
        SessionStage.onboarding => OnboardingView(session: _session),
        SessionStage.ready => Scaffold(
          body: SafeArea(
            child: Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'Your account is ready',
                    style: Theme.of(context).textTheme.headlineMedium,
                  ),
                  const SizedBox(height: 12),
                  Text('${_session.profile!.calories} kcal / day'),
                ],
              ),
            ),
          ),
        ),
        SessionStage.expired => SessionExpiredView(
          onLogin: () {
            _signedOutScreen = 'login';
            _session.loginAgain();
          },
        ),
        SessionStage.retry => Scaffold(
          body: SafeArea(
            child: Center(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    AppFeedbackCard(
                      title: 'Unable to continue',
                      detail: _session.error ?? 'Please try again.',
                    ),
                    const SizedBox(height: 14),
                    AppButton(label: 'Try again', onPressed: _session.retry),
                  ],
                ),
              ),
            ),
          ),
        ),
        SessionStage.signedOut =>
          _signedOutScreen == 'welcome'
              ? WelcomeView(
                  onRegister: () => _show('register'),
                  onLogin: () => _show('login'),
                )
              : PopScope(
                  canPop: false,
                  onPopInvokedWithResult: (didPop, result) {
                    if (!didPop && !_session.busy) _show('welcome');
                  },
                  child: AuthView(
                    key: ValueKey(_signedOutScreen),
                    session: _session,
                    register: _signedOutScreen == 'register',
                    onBack: () => _show('welcome'),
                    onSwitch: () => _show(
                      _signedOutScreen == 'register' ? 'login' : 'register',
                      preserveError: _session.registered,
                    ),
                  ),
                ),
      },
    ),
  );
}
