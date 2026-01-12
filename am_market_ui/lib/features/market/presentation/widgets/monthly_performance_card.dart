import 'package:flutter/material.dart';
import 'package:am_design_system/am_design_system.dart';
import 'package:am_market_ui/models/indices_performance_model.dart';
import 'package:am_market_ui/features/market/presentation/widgets/performance_ranking_dialog.dart';
import 'package:am_market_ui/core/styles/am_text_styles.dart';

class MonthlyPerformanceCard extends StatelessWidget {
  final MonthlyIndicesPerformance data;

  const MonthlyPerformanceCard({Key? key, required this.data}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.darkCard,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.darkDivider.withOpacity(0.1)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '${data.monthName} ${data.year}',
                style: AmTextStyles.subtitle2.copyWith(color: AppColors.textPrimaryDark),
              ),
              IconButton(
                icon: const Icon(Icons.visibility, size: 16, color: AppColors.primary),
                onPressed: () => _showRanking(context),
                tooltip: 'View Full Ranking',
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(),
              ),
            ],
          ),
          const Divider(height: 16),
          
          // Top Performer
          if (data.topPerformer != null)
            _buildPerformerRow(
              'Top:',
              data.topPerformer!,
              AppColors.success,
            ),
            
          const SizedBox(height: 8),
          
          // Worst Performer
          if (data.worstPerformer != null)
            _buildPerformerRow(
              'Worst:',
              data.worstPerformer!,
              AppColors.error,
            ),
        ],
      ),
    );
  }

  Widget _buildPerformerRow(String label, IndexPerformance perf, Color color) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Row(
          children: [
            Text(
              label,
              style: AmTextStyles.caption.copyWith(color: AppColors.textSecondaryDark),
            ),
            const SizedBox(width: 4),
            Text(
              perf.symbol,
              style: AmTextStyles.caption.copyWith(
                color: AppColors.textPrimaryDark,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
        Text(
          '${perf.returnPercentage >= 0 ? '+' : ''}${perf.returnPercentage.toStringAsFixed(2)}%',
          style: AmTextStyles.caption.copyWith(
            color: color,
            fontWeight: FontWeight.bold,
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
