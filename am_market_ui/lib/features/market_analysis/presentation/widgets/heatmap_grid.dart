import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:am_market_common/providers/market_provider.dart';
import 'package:am_market_ui/features/stock_detail/presentation/pages/stock_detail_page.dart';

class HeatmapGrid extends StatelessWidget {
  final List stocks;
  final MarketProvider provider;

  const HeatmapGrid({
    super.key, 
    required this.stocks, 
    required this.provider
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: LayoutBuilder(
        builder: (context, constraints) {
          // Determine column count based on width
          int crossAxisCount = (constraints.maxWidth / 200).floor();
          if (crossAxisCount < 2) crossAxisCount = 2; // Min columns

          return StreamBuilder<Map<String, dynamic>>(
            stream: provider.livePriceStream,
            builder: (context, snapshot) {
              return GridView.builder(
                gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: crossAxisCount,
                  childAspectRatio: 2.2, // Rectangular boxes
                  crossAxisSpacing: 10,
                  mainAxisSpacing: 10,
                ),
                itemCount: stocks.length,
                itemBuilder: (context, index) {
                  final stock = stocks[index];
                  // Merge with live data
                  final liveData = provider.livePrices[stock.symbol];
                  
                  double price = stock.lastPrice;
                  double pChange = stock.pChange;
                  
                  if (liveData != null) {
                      price = (liveData['lastPrice'] as num?)?.toDouble() ?? price;
                      pChange = (liveData['changePercent'] as num?)?.toDouble() ?? pChange;
                  }

                  final isPositive = pChange >= 0;
                  final intensity = (pChange.abs() / 3).clamp(0.2, 1.0); // Simple intensity scaling
                  final baseColor = isPositive ? Colors.green : Colors.red;
                  final color = baseColor.withOpacity(intensity);

                  return InkWell(
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => StockDetailPage(symbol: stock.symbol),
                        ),
                      );
                    },
                    child: Container(
                      decoration: BoxDecoration(
                        color: color,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      padding: const EdgeInsets.all(12),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              // Use flexible to avoid overflow
                              Flexible(
                                child: Text(
                                  stock.symbol,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 14,
                                    color: Colors.white
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              Text(
                                '${isPositive ? '+' : ''}${pChange.toStringAsFixed(2)}%',
                                style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 14,
                                  color: Colors.white
                                ),
                              ),
                            ],
                          ),
                          Row(
                            children: [
                                Text(
                                  NumberFormat.currency(symbol: '₹', locale: 'en_IN').format(price),
                                  style: const TextStyle(fontSize: 12, color: Colors.white70),
                                ),
                            ],
                          )
                        ],
                      ),
                    ),
                  );
                },
              );
            }
          );
        },
      ),
    );
  }
}
