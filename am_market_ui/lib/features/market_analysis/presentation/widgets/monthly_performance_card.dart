import 'package:flutter/material.dart';
import 'package:am_design_system/am_design_system.dart';
import 'package:am_market_common/models/indices_performance_model.dart';
import 'package:am_market_ui/features/market_analysis/presentation/widgets/performance_ranking_dialog.dart';
import 'package:am_market_ui/core/styles/am_text_styles.dart';

class MonthlyPerformanceCard extends StatelessWidget {
  final MonthlyIndicesPerformance data;
  final bool isCompactTable;

  const MonthlyPerformanceCard({Key? key, required this.data, this.isCompactTable = false}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    // Table Mode: Single Line (Top Performer Only)
    if (isCompactTable) {
       final perf = data.topPerformer;
       if (perf == null) return const SizedBox();

       final isPositive = perf.returnPercentage >= 0;
       final bgColor = isPositive 
            ? const Color(0xFF1B5E20).withOpacity(0.3) 
            : const Color(0xFFB71C1C).withOpacity(0.3);
       final textColor = isPositive ? const Color(0xFF69F0AE) : const Color(0xFFFF5252);

       return InkWell(
          onTap: () => _showRanking(context),
          borderRadius: BorderRadius.circular(6),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8), // More internal padding
            decoration: BoxDecoration(
              color: bgColor,
              borderRadius: BorderRadius.circular(6),
              border: Border.all(color: textColor.withOpacity(0.3)),
            ),
            child: Row(
              children: [
                // Full Index Name
                Expanded(
                  child: Text(
                    perf.symbol,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 11, 
                      fontWeight: FontWeight.w600,
                    ),
                    overflow: TextOverflow.visible, // Wrapping allowed if needed, but expanded width should help
                    maxLines: 1,
                  ),
                ),
                const SizedBox(width: 8),
                // Percentage
                Text(
                  '${isPositive ? '+' : ''}${perf.returnPercentage.toStringAsFixed(1)}%',
                  style: TextStyle(
                    color: textColor,
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                 const SizedBox(width: 6),
                 // View Icon
                 Icon(Icons.open_in_new, size: 10, color: Colors.white.withOpacity(0.5))
              ],
            ),
          ),
       );
    }

    // Default Card Mode
    return InkWell(
      onTap: () => _showRanking(context),
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
        decoration: BoxDecoration(
          color: AppColors.darkCard,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.white.withOpacity(0.05)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            // Header
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '${data.monthName.substring(0, 3)} ${data.year}',
                  style: AmTextStyles.caption.copyWith(
                    color: Colors.white54,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Icon(Icons.remove_red_eye_outlined, size: 12, color: Colors.white24),
              ],
            ),
            
            const Spacer(),

            if (data.topPerformer != null)
              _buildCompactRow("Top", data.topPerformer!),
              
            const SizedBox(height: 4),
            
            if (data.worstPerformer != null)
              _buildCompactRow("Bot", data.worstPerformer!),
          ],
        ),
      ),
    );
  }

  Widget _buildCompactRow(String label, IndexPerformance perf) {
    final isPositive = perf.returnPercentage >= 0;
    // Darker Green/Red for background, brighter for text
    final bgColor = isPositive 
        ? const Color(0xFF1B5E20).withOpacity(0.4) // Dark Green
        : const Color(0xFFB71C1C).withOpacity(0.4); // Dark Red
    final textColor = isPositive 
        ? const Color(0xFF69F0AE) // Bright Green
        : const Color(0xFFFF5252); // Bright Red

    return Row(
      children: [
        // Label (Top/Bot)
        SizedBox(
          width: 24,
          child: Text(
            label,
            style: TextStyle(color: Colors.white38, fontSize: 10),
          ),
        ),
        // Badge
        Expanded(
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
            decoration: BoxDecoration(
              color: bgColor,
              borderRadius: BorderRadius.circular(4),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    perf.symbol,
                    style: TextStyle(
                      color: Colors.white.withOpacity(0.9),
                      fontSize: 10,
                      fontWeight: FontWeight.w500,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ),
                Text(
                  '${isPositive ? '+' : ''}${perf.returnPercentage.toStringAsFixed(1)}%',
                  style: TextStyle(
                    color: textColor,
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  void _showRanking(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => PerformanceRankingDialog(data: data),
    );
  }
}
