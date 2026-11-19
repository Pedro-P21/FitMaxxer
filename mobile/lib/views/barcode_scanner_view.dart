import 'dart:async';

import 'package:material_ui/material_ui.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../theme/app_colors.dart';
import '../widgets/app_button.dart';

bool validProductBarcode(String? value) =>
    value != null && RegExp(r'^\d{8,14}$').hasMatch(value);

class BarcodeScannerView extends StatefulWidget {
  const BarcodeScannerView({super.key});
  @override
  State<BarcodeScannerView> createState() => _BarcodeScannerViewState();
}

class _BarcodeScannerViewState extends State<BarcodeScannerView>
    with WidgetsBindingObserver {
  final _controller = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
    formats: [
      BarcodeFormat.ean8,
      BarcodeFormat.ean13,
      BarcodeFormat.upcA,
      BarcodeFormat.upcE,
      BarcodeFormat.itf14,
      BarcodeFormat.code128,
    ],
  );
  bool _detected = false;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  Future<void> _start() async {
    try {
      await _controller.start();
    } on MobileScannerException {
      /* The scanner's errorBuilder displays camera failures. */
    }
  }

  Future<void> _stop() async {
    try {
      await _controller.stop();
    } on MobileScannerException {
      /* Camera may already be stopped by the OS. */
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (!_controller.value.hasCameraPermission || _detected) return;
    if (state == AppLifecycleState.resumed) {
      unawaited(_start());
    } else if (state == AppLifecycleState.inactive ||
        state == AppLifecycleState.paused) {
      unawaited(_stop());
    }
  }

  void _onDetect(BarcodeCapture capture) {
    if (_detected || !mounted) return;
    final code = capture.barcodes
        .map((b) => b.rawValue)
        .where(validProductBarcode)
        .firstOrNull;
    if (code == null) return;
    _detected = true;
    unawaited(_stop());
    Navigator.of(context).pop(code);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    unawaited(_controller.dispose());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Scan a barcode')),
    body: SafeArea(
      child: Column(
        children: [
          const Padding(
            padding: EdgeInsets.all(20),
            child: Text('Point your camera at the product’s barcode.'),
          ),
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(24),
              child: MobileScanner(
                controller: _controller,
                onDetect: _onDetect,
                errorBuilder: (context, error) => ColoredBox(
                  color: AppColors.background,
                  child: Center(
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Text(
                            'Camera unavailable. Allow camera access in your phone’s Settings, then try again.',
                          ),
                          const SizedBox(height: 16),
                          AppButton(label: 'Try again', onPressed: _start),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(20),
            child: AppButton(
              label: 'Cancel',
              variant: AppButtonVariant.secondary,
              onPressed: () => Navigator.pop(context),
            ),
          ),
        ],
      ),
    ),
  );
}
