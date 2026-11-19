import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/theme/app_theme.dart';
import 'package:mobile/widgets/app_button.dart';
import 'package:mobile/widgets/app_feedback_card.dart';
import 'package:mobile/widgets/app_text_field.dart';

Widget host(Widget child, {double scale = 1}) => MaterialApp(
  theme: AppTheme.light,
  home: Scaffold(
    body: MediaQuery(
      data: MediaQueryData(textScaler: TextScaler.linear(scale)),
      child: Center(child: SizedBox(width: 350, child: child)),
    ),
  ),
);

void main() {
  testWidgets('buttons invoke actions and block disabled or loading taps', (
    tester,
  ) async {
    var calls = 0;
    for (final variant in AppButtonVariant.values) {
      await tester.pumpWidget(
        host(
          AppButton(
            label: 'Continue',
            onPressed: () => calls++,
            variant: variant,
          ),
        ),
      );
      await tester.tap(find.text('Continue'));
      await tester.pump();
    }
    expect(calls, 3);

    await tester.pumpWidget(
      host(const AppButton(label: 'Continue', onPressed: null)),
    );
    await tester.tap(find.text('Continue'));
    expect(calls, 3);

    await tester.pumpWidget(
      host(
        AppButton(label: 'Continue', onPressed: () => calls++, isLoading: true),
      ),
    );
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    await tester.tap(find.byType(TextButton));
    expect(calls, 3);
  });

  testWidgets('field validates edits, controller changes, and form reset', (
    tester,
  ) async {
    final form = GlobalKey<FormState>();
    final controller = TextEditingController();
    addTearDown(controller.dispose);
    await tester.pumpWidget(
      host(
        Form(
          key: form,
          child: AppTextField(
            label: 'Email',
            controller: controller,
            validator: (value) => value == 'alex@example.com'
                ? null
                : 'Enter a valid email address.',
          ),
        ),
      ),
    );
    await tester.enterText(find.byType(TextField), 'alex@');
    expect(form.currentState!.validate(), isFalse);
    await tester.pump();
    expect(find.text('Enter a valid email address.'), findsOneWidget);
    controller.text = 'alex@example.com';
    expect(form.currentState!.validate(), isTrue);
    await tester.pump();
    expect(find.text('Enter a valid email address.'), findsNothing);
    form.currentState!.reset();
    await tester.pump();
    expect(controller.text, isEmpty);
  });

  testWidgets('password field masks input and suppresses suggestions', (
    tester,
  ) async {
    await tester.pumpWidget(
      host(const AppTextField(label: 'Password', obscureText: true)),
    );
    await tester.enterText(find.byType(TextField), 'secret-password');
    final field = tester.widget<TextField>(find.byType(TextField));
    expect(field.obscureText, isTrue);
    expect(field.enableSuggestions, isFalse);
    expect(field.autocorrect, isFalse);
  });

  testWidgets('components wrap long content at large text sizes', (
    tester,
  ) async {
    await tester.pumpWidget(
      host(
        SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              AppButton(
                label: 'Already registered? Log in to your existing account',
                onPressed: () {},
              ),
              const AppTextField(label: 'Email or username'),
              const AppFeedbackCard(
                title: 'Email, username or password is incorrect',
                detail: 'Check your details and try again.',
              ),
            ],
          ),
        ),
        scale: 2,
      ),
    );
    expect(tester.takeException(), isNull);
  });
}
