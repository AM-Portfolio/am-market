import 'package:flutter/material.dart';
import 'package:am_market_common/models/market_data.dart';
import 'package:intl/intl.dart';

/// Widget showing top gainers or losers
class TopMoversWidget extends StatelessWidget {
  final List<StockIndicesMarketData> movers;
  final String title;
  final bool isGainers;

  const TopMoversWidget({
    required this.movers,
    required this.title,
    this.isGainers = true,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final displayMovers = movers.take(5).toList();
    final numberFormat = NumberFormat('#,##,###.##', 'en_IN');

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF1A1D26).withOpacity(0.6),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: Colors.white.withOpacity(0.1),
          width: 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          Row(
            children: [
              Icon(
                isGainers ? Icons.trending_up : Icons.trending_down,
                color: isGainers ? const Color(0xFF00FF88) : const Color(0xFFFF6B6B),
                size: 20,
              ),
              const SizedBox(width: 8),
              Text(
                title,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          
          // List of movers
          if (displayMovers.isEmpty)
            Center(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Text(
                  'No data available',
                  style: TextStyle(
                    color: Colors.white.withOpacity(0.5),
                    fontSize: 14,
                  ),
                ),
              ),
            )
          else
            ...displayMovers.asMap().entries.map((entry) {
              final index = entry.key;
              final stock = entry.value;
              final accentColor = isGainers 
                  ? const Color(0xFF00FF88) 
                  : const Color(0xFFFF6B6B);

              return Padding(
                padding: EdgeInsets.only(bottom: index < displayMovers.length - 1 ? 12 : 0),
                child: Row(
                  children: [
                    // Rank
                    Container(
                      width: 24,
                      height: 24,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: accentColor.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(
                        '${index + 1}',
                        style: TextStyle(
                          color: accentColor,
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    
                    // Symbol and price
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            stock.indexSymbol,
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                            ),
                            overflow: TextOverflow.ellipsis,
                          ),
                          const SizedBox(height: 2),
                          Text(
                            '₹${numberFormat.format(stock.lastPrice)}',
                            style: TextStyle(
                              color: Colors.white.withOpacity(0.6),
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                    ),
                    
                    // Change percentage
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: accentColor.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        '${isGainers ? '+' : ''}${stock.pChange.toStringAsFixed(2)}%',
                        style: TextStyle(
                          color: accentColor,
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
        ],
      ),
    );
  }
}
