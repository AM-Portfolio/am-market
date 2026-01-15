import 'package:flutter/material.dart';
import 'package:am_design_system/am_design_system.dart';
import 'package:am_market_common/models/market_data.dart';

/// Simple indices comparison widget showing 3 indices side-by-side
class IndicesComparisonWidget extends StatelessWidget {
  final List<StockIndicesMarketData> indices;
  final List<String> selectedSymbols;

  const IndicesComparisonWidget({
    required this.indices,
    required this.selectedSymbols,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    
    // Filter to show only selected indices
    final displayIndices = indices
        .where((index) => selectedSymbols.contains(index.indexSymbol))
        .toList();

    if (displayIndices.isEmpty) {
      return Center(
        child: Text(
          'No indices selected',
          style: TextStyle(
            color: Colors.white.withOpacity(0.5),
            fontSize: 14,
          ),
        ),
      );
    }

    return Row(
      children: displayIndices.map((data) {
        final isPositive = data.pChange >= 0;
        final colorIndex = displayIndices.indexOf(data);
        final colors = [
          const Color(0xFF00D1FF), // Cyan
          const Color(0xFFFF6B6B), // Red  
          const Color(0xFF00FF88), // Green
        ];
        final accentColor = colors[colorIndex % colors.length];

        return Expanded(
          child: Padding(
            padding: const EdgeInsets.only(right: 16),
            child: GlassCard(
              padding: const EdgeInsets.all(20),
              colorScheme: colorIndex == 0 ? 'primary' : (colorIndex == 1 ? 'accent' : 'success'),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Symbol name
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text(
                          data.indexSymbol,
                          style: TextStyle(
                            color: isDark ? Colors.white : Colors.black87,
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                          ),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      Icon(
                        isPositive ? Icons.trending_up : Icons.trending_down,
                        color: accentColor,
                        size: 20,
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  
                  // Price
                  Text(
                    data.lastPrice.toStringAsFixed(2),
                    style: TextStyle(
                      color: isDark ? Colors.white : Colors.black87,
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  
                  // Change
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: accentColor.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      '${isPositive ? '+' : ''}${data.pChange.toStringAsFixed(2)}%',
                      style: TextStyle(
                        color: accentColor,
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  
                  const SizedBox(height: 12),
                  
                  // Simple bar visualization
                  Container(
                    height: 4,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(2),
                      gradient: LinearGradient(
                        colors: [
                          accentColor.withOpacity(0.3),
                          accentColor,
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      }).toList(),
    );
  }
}
