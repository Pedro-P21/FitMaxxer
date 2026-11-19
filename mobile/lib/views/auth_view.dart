import 'package:material_ui/material_ui.dart';

import '../providers/session_provider.dart';
import '../theme/app_colors.dart';
import '../theme/app_theme.dart';
import '../widgets/app_button.dart';
import '../widgets/app_feedback_card.dart';
import '../widgets/app_text_field.dart';
import '../widgets/flow_scaffold.dart';

class AuthView extends StatefulWidget {
  const AuthView({
    super.key,
    required this.session,
    required this.register,
    required this.onBack,
    required this.onSwitch,
  });
  final SessionProvider session;
  final bool register;
  final VoidCallback onBack, onSwitch;
  @override
  State<AuthView> createState() => _AuthViewState();
}

class _AuthViewState extends State<AuthView> {
  final _form = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _identifier = TextEditingController();
  final _password = TextEditingController();
  bool _submitted = false;
  bool _invalid = false;

  Future<void> _submit() async {
    if (widget.session.busy) return;
    FocusScope.of(context).unfocus();
    setState(() => _submitted = true);
    final valid = _form.currentState!.validate();
    setState(() => _invalid = !valid);
    if (!valid) return;
    await widget.session.authenticate(
      _identifier.text,
      _password.text,
      email: widget.register ? _email.text : null,
    );
    if (mounted) _password.clear();
    if (mounted &&
        widget.register &&
        widget.session.registered &&
        widget.session.stage == SessionStage.signedOut) {
      widget.onSwitch();
    }
  }

  @override
  void dispose() {
    _email.dispose();
    _identifier.dispose();
    _password.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final registering = widget.register;
    final session = widget.session;
    return Form(
      key: _form,
      autovalidateMode: _submitted
          ? AutovalidateMode.onUserInteraction
          : AutovalidateMode.disabled,
      child: FlowScaffold(
        onBack: session.busy ? null : widget.onBack,
        eyebrow: registering ? 'WELCOME TO FITMAXXER' : 'LOG IN',
        title: registering ? 'Create your account' : 'Good to see you',
        actions: [
          AppButton(
            label: registering ? 'Create account' : 'Log in',
            isLoading: session.busy,
            onPressed: _submit,
          ),
          AppButton(
            label: registering
                ? 'Already registered? Log in'
                : 'New here? Create an account',
            variant: AppButtonVariant.secondary,
            onPressed: session.busy ? null : widget.onSwitch,
          ),
        ],
        children: [
          Text(
            registering
                ? 'Your nutrition journey starts here.'
                : 'Pick up right where you left off.',
          ),
          AutofillGroup(
            child: Column(
              children: [
                if (registering) ...[
                  AppTextField(
                    label: 'Email',
                    controller: _email,
                    enabled: !session.busy,
                    keyboardType: TextInputType.emailAddress,
                    textInputAction: TextInputAction.next,
                    autofillHints: const [AutofillHints.email],
                    onChanged: (_) => session.clearError(),
                    validator: (value) =>
                        RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$')
                            .hasMatch(value?.trim() ?? '')
                        ? null
                        : 'Enter a valid email address.',
                  ),
                  const SizedBox(height: 14),
                ],
                AppTextField(
                  label: registering ? 'Username' : 'Email or username',
                  controller: _identifier,
                  enabled: !session.busy,
                  autofillHints: const [AutofillHints.username],
                  textInputAction: TextInputAction.next,
                  onChanged: (_) => session.clearError(),
                  validator: (v) => v == null || v.trim().isEmpty
                      ? 'Enter ${registering ? 'a username' : 'your email or username'}.'
                      : null,
                ),
                const SizedBox(height: 14),
                AppTextField(
                  label: 'Password',
                  controller: _password,
                  enabled: !session.busy,
                  obscureText: true,
                  autofillHints: [
                    registering
                        ? AutofillHints.newPassword
                        : AutofillHints.password,
                  ],
                  textInputAction: TextInputAction.done,
                  onFieldSubmitted: (_) => _submit(),
                  onChanged: (_) => session.clearError(),
                  validator: (v) => v == null || v.isEmpty
                      ? 'Enter your password.'
                      : registering && v.length < 8
                      ? 'Use at least 8 characters for your password.'
                      : null,
                ),
              ],
            ),
          ),
          if (registering)
            Text(
              'Use at least 8 characters for your password.',
              style: AppTheme.textStyle(12, color: AppColors.muted),
            ),
          if (registering && _invalid)
            const AppFeedbackCard(
              title: 'Check your details',
              detail: 'Your account has not been created yet.',
            ),
          if (session.error != null)
            AppFeedbackCard(
              title: session.error!,
              detail: session.registered
                  ? 'Your account is ready. Log in to continue.'
                  : registering
                  ? 'Your account has not been created yet.'
                  : 'Check your details and try again.',
            ),
          if (!registering && session.error != null)
            TextButton(
              onPressed: () {
                _password.clear();
                session.clearError();
              },
              child: const Text('Clear the password and retry'),
            ),
          if (registering)
            Text(
              'By continuing, you agree to keep your account details secure.',
              style: AppTheme.textStyle(12, color: AppColors.muted),
            ),
        ],
      ),
    );
  }
}
