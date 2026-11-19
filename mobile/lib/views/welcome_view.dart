import 'package:material_ui/material_ui.dart';

import '../widgets/app_button.dart';
import '../widgets/app_logo.dart';
import '../widgets/flow_scaffold.dart';

class WelcomeView extends StatelessWidget {
  const WelcomeView({
    super.key,
    required this.onRegister,
    required this.onLogin,
  });
  final VoidCallback onRegister, onLogin;
  @override
  Widget build(BuildContext context) => FlowScaffold(
    eyebrow: 'NUTRITION, IN YOUR FLOW',
    title: 'FitMaxxer',
    bodySpacing: 22,
    actions: [
      AppButton(label: 'Get started', onPressed: onRegister),
      AppButton(
        label: 'I already have an account',
        onPressed: onLogin,
        variant: AppButtonVariant.secondary,
      ),
    ],
    children: [
      const AppLogo(),
      Text(
        'A little awareness.\nA stronger everyday.',
        style: Theme.of(context).textTheme.headlineLarge,
      ),
      Text(
        'Log what you eat, find your balance and keep your favorite meals close.',
        style: Theme.of(context).textTheme.bodyLarge,
      ),
    ],
  );
}
