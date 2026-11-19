// Run from mobile/: flutter test tool/render_app_icon.dart
import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';
import 'package:mobile/theme/app_colors.dart';
import 'package:mobile/widgets/app_logo.dart';

void main() {
  testWidgets('render the shared welcome logo as an opaque launcher icon', (
    tester,
  ) async {
    final key = GlobalKey();
    await tester.pumpWidget(
      Directionality(
        textDirection: TextDirection.ltr,
        child: Center(
          child: RepaintBoundary(
            key: key,
            child: const SizedBox.square(
              dimension: 256,
              child: ColoredBox(
                color: AppColors.background,
                child: Padding(
                  padding: EdgeInsets.all(24),
                  child: FittedBox(
                    child: SizedBox(
                      width: 240,
                      height: 200,
                      child: AppLogo(showWordmark: false),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Fitmaxxer'), findsNothing);
    final boundary =
        key.currentContext!.findRenderObject()! as RenderRepaintBoundary;
    await tester.runAsync(() async {
      final image = await boundary.toImage(pixelRatio: 4);
      final data = await image.toByteData(format: ui.ImageByteFormat.png);
      final file = File('assets/app_icon/app_icon.png');
      file.parent.createSync(recursive: true);
      file.writeAsBytesSync(data!.buffer.asUint8List());
      image.dispose();
    });
  });
}
