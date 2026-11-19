/// UI names are deliberately independent of the backend's goal identifiers.
enum GoalOption {
  higherDeficit('Higher deficit cutting', 'AGGRESSIVE_CUTTING', -500),
  moderateCutting('Moderate cutting', 'MODERATE_CUTTING', -300),
  slightCutting('Slight cutting', 'LIGHT_CUTTING', -200),
  maintenance('Maintenance', 'MAINTAIN_WEIGHT', 0),
  leanBulking('Lean bulking', 'LIGHT_BULK', 200),
  moderateBulking('Moderate bulking', 'MODERATE_BULK', 300),
  higherSurplus('Higher surplus bulking', 'AGGRESSIVE_BULK', 500);

  const GoalOption(this.label, this.apiValue, this.offset);
  final String label;
  final String apiValue;
  final int offset;
  String get description => offset == 0
      ? 'Matches your estimated maintenance calories'
      : '${offset.abs()} kcal ${offset < 0 ? 'below' : 'above'} maintenance';
}
