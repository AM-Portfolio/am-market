import 'package:flutter/material.dart';

class MarketMoversView extends StatelessWidget {
  final List<Map<String, dynamic>> gainers;
  final List<Map<String, dynamic>> losers;
  final bool isLoading;

  const MarketMoversView({
    super.key,
    required this.gainers,
    required this.losers,
    this.isLoading = false,
  });

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.trending_up, color: Colors.blueAccent, size: 24),
              SizedBox(width: 8),
              Text(
                'Market Movers',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(child: _buildMoversList('Top Gainers', gainers, Colors.green)),
              const SizedBox(width: 16),
              Expanded(child: _buildMoversList('Top Losers', losers, Colors.red)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMoversList(String title, List<Map<String, dynamic>> data, Color accentColor) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: accentColor.withOpacity(0.05), // Light tint background
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: accentColor.withOpacity(0.2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: TextStyle(
              color: accentColor,
              fontWeight: FontWeight.bold,
              fontSize: 16,
            ),
          ),
          const SizedBox(height: 12),
          ...data.take(5).map((stock) {
            final symbol = stock['symbol'] ?? 'N/A';
            final pChange = (stock['pChange'] ?? 0.0).toDouble();
            final ltp = (stock['lastPrice'] ?? 0.0).toDouble();

            return Padding(
              padding: const EdgeInsets.symmetric(vertical: 6),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Text(
                      symbol,
                      style: const TextStyle(color: Colors.black87, fontSize: 13, fontWeight: FontWeight.bold),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(
                        ltp.toStringAsFixed(2),
                        style: TextStyle(color: Colors.grey.shade700, fontSize: 12),
                      ),
                      Text(
                        '${pChange >= 0 ? '+' : ''}${pChange.toStringAsFixed(2)}%',
                        style: TextStyle(
                          color: accentColor,
                          fontWeight: FontWeight.bold,
                          fontSize: 12,
                        ),
                      ),
                    ],
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
