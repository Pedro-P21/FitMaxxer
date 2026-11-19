import 'package:material_ui/material_ui.dart';

import '../models/log_history.dart';
import '../widgets/app_button.dart';
import '../widgets/flow_scaffold.dart';

class HistoryMonthView extends StatefulWidget {
  const HistoryMonthView({
    super.key,
    required this.selected,
    required this.earliest,
    required this.latest,
  });
  final DateTime selected, earliest, latest;
  @override
  State<HistoryMonthView> createState() => _HistoryMonthViewState();
}

class _HistoryMonthViewState extends State<HistoryMonthView> {
  late int _year, _month;
  @override
  void initState() {
    super.initState();
    _year = widget.selected.year;
    _month = widget.selected.month;
  }

  bool _enabled(int year, int month) {
    final date = DateTime(year, month);
    return !date.isBefore(widget.earliest) && !date.isAfter(widget.latest);
  }

  void _changeYear(int year) {
    setState(() {
      _year = year;
      if (_year == widget.earliest.year && _month < widget.earliest.month) {
        _month = widget.earliest.month;
      }
      if (_year == widget.latest.year && _month > widget.latest.month) {
        _month = widget.latest.month;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    // Three-year rows scroll through the entire supported history window.
    final firstYear = (_year - 1).clamp(
      widget.earliest.year,
      widget.latest.year - 2,
    );
    final selected = DateTime(_year, _month);
    return FlowScaffold(
      eyebrow: 'HISTORY',
      title: 'Choose a month',
      backLabel: 'History',
      onBack: () => Navigator.pop(context),
      actions: [
        AppButton(
          label: 'View ${monthLabel(selected)}',
          onPressed: () => Navigator.pop(context, selected),
        ),
      ],
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                'Year',
                style: Theme.of(context).textTheme.titleMedium,
              ),
            ),
            IconButton(
              tooltip: 'Earlier years',
              onPressed: _year > widget.earliest.year
                  ? () => _changeYear(_year - 1)
                  : null,
              icon: const Icon(Icons.chevron_left_rounded),
            ),
            IconButton(
              tooltip: 'Later years',
              onPressed: _year < widget.latest.year
                  ? () => _changeYear(_year + 1)
                  : null,
              icon: const Icon(Icons.chevron_right_rounded),
            ),
          ],
        ),
        Row(
          children: [
            for (var year = firstYear; year < firstYear + 3; year++) ...[
              Expanded(
                child: AppButton(
                  label: '${year == _year ? '✓  ' : ''}$year',
                  variant: AppButtonVariant.secondary,
                  onPressed: () => _changeYear(year),
                ),
              ),
              if (year < firstYear + 2) const SizedBox(width: 8),
            ],
          ],
        ),
        Text('Month', style: Theme.of(context).textTheme.titleMedium),
        for (var row = 0; row < 4; row++)
          Row(
            children: [
              for (var col = 0; col < 3; col++) ...[
                if (col > 0) const SizedBox(width: 8),
                Expanded(
                  child: AppButton(
                    label:
                        '${row * 3 + col + 1 == _month ? '✓  ' : ''}${monthNames[row * 3 + col].substring(0, 3)}',
                    variant: AppButtonVariant.secondary,
                    onPressed: _enabled(_year, row * 3 + col + 1)
                        ? () => setState(() => _month = row * 3 + col + 1)
                        : null,
                  ),
                ),
              ],
            ],
          ),
      ],
    );
  }
}
