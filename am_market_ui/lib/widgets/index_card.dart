import 'package:flutter/material.dart';
import 'package:am_market_common/models/market_data.dart';
import 'package:intl/intl.dart';

/// Individual index card showing name, price, and change
class IndexCard extends StatelessWidget {
  final StockIndicesMarketData data;

  const IndexCard({
    required this.data,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final isPositive = data.pChange >= 0;
    final accentColor = isPositive 
        ? const Color(0xFF00FF88) // Green
        : const Color(0xFFFF6B6B); // Red

    final numberFormat = NumberFormat('#,##,###.##', 'en_IN');

    return Container(
      width: 160,
      margin: const EdgeInsets.only(right: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF1A1D26).withOpacity(0.6),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: accentColor.withOpacity(0.3),
          width: 1,
        ),
        boxShadow: [
          BoxShadow(
            color: accentColor.withOpacity(0.1),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          // Index Name
          Text(
            data.indexSymbol,
            style: TextStyle(
              color: Colors.white.withOpacity(0.8),
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 8),
          
          // Price
          Text(
            '₹${numberFormat.format(data.lastPrice)}',
            style: const TextStyle(
              color: Colors.white,
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 4),
          
          // Change Amount
          Row(
            children: [
              Icon(
                isPositive ? Icons.arrow_upward : Icons.arrow_downward,
                color: accentColor,
                size: 14,
              ),
              const SizedBox(width: 4),
              Text(
                '${isPositive ? '+' : ''}${numberFormat.format(data.change)}',
                style: TextStyle(
                  color: accentColor,
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          const SizedBox(height: 2),
          
          // Percentage
          Text(
            '${isPositive ? '+' : ''}${data.pChange.toStringAsFixed(2)}%',
            style: TextStyle(
              color: accentColor,
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}
