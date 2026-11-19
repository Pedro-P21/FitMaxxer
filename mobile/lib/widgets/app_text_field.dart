import 'package:material_ui/material_ui.dart';
import 'package:flutter/services.dart';

import '../theme/app_colors.dart';
import '../theme/app_theme.dart';

/// Editable Figma field with native selection, autofill and Form validation.
class AppTextField extends StatefulWidget {
  const AppTextField({
    super.key,
    required this.label,
    this.controller,
    this.hintText,
    this.obscureText = false,
    this.enabled = true,
    this.keyboardType,
    this.textInputAction,
    this.autofillHints,
    this.validator,
    this.onChanged,
    this.onFieldSubmitted,
    this.focusNode,
    this.inputFormatters,
    this.autovalidateMode = AutovalidateMode.disabled,
  });

  final String label;
  final TextEditingController? controller;
  final String? hintText;
  final bool obscureText;
  final bool enabled;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final Iterable<String>? autofillHints;
  final FormFieldValidator<String>? validator;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onFieldSubmitted;
  final FocusNode? focusNode;
  final List<TextInputFormatter>? inputFormatters;
  final AutovalidateMode autovalidateMode;

  @override
  State<AppTextField> createState() => _AppTextFieldState();
}

class _AppTextFieldState extends State<AppTextField> {
  TextEditingController? _ownedController;
  FocusNode? _ownedFocusNode;
  late String _initialValue;

  TextEditingController get _controller =>
      widget.controller ?? _ownedController!;
  FocusNode get _focusNode => widget.focusNode ?? _ownedFocusNode!;

  @override
  void initState() {
    super.initState();
    if (widget.controller == null) _ownedController = TextEditingController();
    if (widget.focusNode == null) _ownedFocusNode = FocusNode();
    _initialValue = _controller.text;
  }

  @override
  void didUpdateWidget(covariant AppTextField oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.controller != widget.controller) {
      final oldValue = oldWidget.controller?.value ?? _ownedController!.value;
      _ownedController?.dispose();
      _ownedController = widget.controller == null
          ? TextEditingController.fromValue(oldValue)
          : null;
      _initialValue = _controller.text;
    }
    if (oldWidget.focusNode != widget.focusNode) {
      _ownedFocusNode?.dispose();
      _ownedFocusNode = widget.focusNode == null ? FocusNode() : null;
    }
  }

  @override
  void dispose() {
    _ownedController?.dispose();
    _ownedFocusNode?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => FormField<String>(
    initialValue: _initialValue,
    enabled: widget.enabled,
    autovalidateMode: widget.autovalidateMode,
    validator: (_) => widget.validator?.call(_controller.text),
    onReset: () => _controller.text = _initialValue,
    builder: (field) => Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        AnimatedBuilder(
          animation: _focusNode,
          builder: (context, child) => Container(
            width: double.infinity,
            constraints: const BoxConstraints(minHeight: 74),
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: AppColors.white,
              borderRadius: BorderRadius.circular(AppTheme.componentRadius),
              border: Border.all(
                color: field.hasError
                    ? AppColors.destructive
                    : _focusNode.hasFocus
                    ? AppColors.blue
                    : AppColors.line,
              ),
            ),
            child: child,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                widget.label,
                style: AppTheme.textStyle(
                  11,
                  weight: FontWeight.w500,
                  color: AppColors.muted,
                ),
              ),
              const SizedBox(height: 3),
              Semantics(
                label: widget.label,
                child: TextField(
                  controller: _controller,
                  focusNode: _focusNode,
                  enabled: widget.enabled,
                  obscureText: widget.obscureText,
                  autocorrect: !widget.obscureText,
                  enableSuggestions: !widget.obscureText,
                  keyboardType: widget.keyboardType,
                  textInputAction: widget.textInputAction,
                  autofillHints: widget.autofillHints,
                  inputFormatters: widget.inputFormatters,
                  onChanged: (value) {
                    field.didChange(value);
                    widget.onChanged?.call(value);
                  },
                  onSubmitted: widget.onFieldSubmitted,
                  style: AppTheme.textStyle(16, weight: FontWeight.w500),
                  decoration: InputDecoration(
                    hintText: widget.hintText,
                    isDense: true,
                    border: InputBorder.none,
                    enabledBorder: InputBorder.none,
                    focusedBorder: InputBorder.none,
                    disabledBorder: InputBorder.none,
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
              ),
            ],
          ),
        ),
        if (field.errorText != null) ...[
          const SizedBox(height: 8),
          Semantics(
            liveRegion: true,
            child: Text(
              field.errorText!,
              style: AppTheme.textStyle(12, color: AppColors.destructive),
            ),
          ),
        ],
      ],
    ),
  );
}
