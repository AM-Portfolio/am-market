import 'package:flutter/material.dart';

class HeatmapFilters extends StatelessWidget {
  final String timeFrame;
  final ValueChanged<String?> onTimeFrameChanged;
  final String? percentFilter;
  final ValueChanged<String?> onPercentFilterChanged;
  
  final List<String> timeFrames;
  final List<String> filters;

  const HeatmapFilters({
    super.key,
    required this.timeFrame,
    required this.onTimeFrameChanged,
    required this.percentFilter,
    required this.onPercentFilterChanged,
    this.timeFrames = const ['5M', '10M', '15M', '30M', '1H', '1D'],
    this.filters = const ['Above +5%', '+2 to +5%', '0 to +2%', '0 to -2%', '-2 to -5%', 'Below -5%'],
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      width: double.infinity,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4)),
        ],
      ),
      child: Wrap(
        spacing: 20,
        runSpacing: 10,
        crossAxisAlignment: WrapCrossAlignment.center,
        alignment: WrapAlignment.spaceBetween,
        children: [
          // Time Frame Dropdown
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            decoration: BoxDecoration(
              border: Border.all(color: Colors.grey.shade300),
              borderRadius: BorderRadius.circular(8),
              color: Colors.grey.shade50,
            ),
            child: DropdownButton<String>(
              value: timeFrame,
              dropdownColor: Colors.white,
              style: const TextStyle(color: Colors.black87, fontWeight: FontWeight.bold),
              underline: const SizedBox(),
              items: timeFrames.map((tf) => DropdownMenuItem(value: tf, child: Text(tf))).toList(),
              onChanged: onTimeFrameChanged,
            ),
          ),
          
          // Percent Filters
          Wrap(
            spacing: 8,
            children: filters.map((f) {
              final isSelected = percentFilter == f;
              Color color;
              Color textColor = isSelected ? Colors.white : Colors.black87;
              
                if (f.contains('Above')) color = Colors.green[700]!;
                else if (f.contains('+2')) color = Colors.green[500]!;
                else if (f.contains('0 to +2')) color = Colors.green[300]!;
                else if (f.contains('0 to -2')) color = Colors.red[300]!;
                else if (f.contains('-2 to')) color = Colors.red[500]!;
                else color = Colors.red[900]!;

              return FilterChip(
                label: Text(f, style: const TextStyle(fontSize: 12)),
                selected: isSelected,
                onSelected: (selected) {
                  onPercentFilterChanged(selected ? f : null);
                },
                backgroundColor: Colors.grey.shade100,
                selectedColor: color,
                checkmarkColor: Colors.white,
                labelStyle: TextStyle(color: textColor),
                side: BorderSide.none,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }
}
