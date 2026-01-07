import 'package:flutter/material.dart';

class TimeRangeSelector extends StatelessWidget {
  final String selectedRange;
  final Function(String) onRangeSelected;
  final List<String> ranges;

  const TimeRangeSelector({
    super.key,
    required this.selectedRange,
    required this.onRangeSelected,
    this.ranges = const ['10m', '15m', '30m', '1H', '4H', '1D', '1W', '1M', '5Y'],
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: ranges.map((range) => _buildRangeButton(range)).toList(),
        ),
      ),
    );
  }

  Widget _buildRangeButton(String range) {
    bool isSelected = selectedRange == range;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4.0),
      child: ElevatedButton(
        style: ElevatedButton.styleFrom(
          backgroundColor: isSelected ? Colors.blueAccent : const Color(0xFF2E2E3E),
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          minimumSize: const Size(0, 32), // Compact height
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
        onPressed: () {
          if (!isSelected) {
            onRangeSelected(range);
          }
        },
        child: Text(range, style: const TextStyle(fontSize: 12)),
      ),
    );
  }
}
