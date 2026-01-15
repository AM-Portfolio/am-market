import 'package:flutter/material.dart';
import 'package:am_design_system/am_design_system.dart';
import 'package:am_market_common/models/indices_performance_model.dart';
import 'dart:ui';
import 'package:am_market_ui/core/styles/am_text_styles.dart';

class PerformanceRankingDialog extends StatelessWidget {
  final MonthlyIndicesPerformance data;

  const PerformanceRankingDialog({Key? key, required this.data}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    // Glassmorphism effect
    return BackdropFilter(
      filter: ImageFilter.blur(sigmaX: 5, sigmaY: 5),
      child: Dialog(
        backgroundColor: AppColors.darkCard.withOpacity(0.9),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        child: Container(
          width: 500,
          constraints: BoxConstraints(
            maxHeight: MediaQuery.of(context).size.height * 0.8,
          ),
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Performance Ranking - ${data.monthName} ${data.year}',
                    style: AmTextStyles.h6.copyWith(color: AppColors.textPrimaryDark),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, color: AppColors.textSecondaryDark),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
              const Divider(height: 24),
              
              // List
              Expanded(
                child: ListView.separated(
                  itemCount: data.allIndices.length,
                  separatorBuilder: (context, index) => const Divider(height: 1),
                  itemBuilder: (context, index) {
                    final perf = data.allIndices[index];
                    final isPositive = perf.returnPercentage >= 0;
                    final rank = index + 1;
                    
                    return ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: CircleAvatar(
                        backgroundColor: isPositive 
                            ? AppColors.success.withOpacity(0.1) 
                            : AppColors.error.withOpacity(0.1),
                        child: Text(
                          '$rank',
                          style: AmTextStyles.body2.copyWith(
                            color: isPositive ? AppColors.success : AppColors.error,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      title: Text(
                        perf.symbol,
                        style: AmTextStyles.body1.copyWith(color: AppColors.textPrimaryDark),
                      ),
                      trailing: Text(
                        '${isPositive ? '+' : ''}${perf.returnPercentage.toStringAsFixed(2)}%',
                        style: AmTextStyles.body1.copyWith(
                          color: isPositive ? AppColors.success : AppColors.error,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
