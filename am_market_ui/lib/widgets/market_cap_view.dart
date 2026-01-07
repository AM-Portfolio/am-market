import 'package:flutter/material.dart';

class MarketCapView extends StatelessWidget {
  final Map<String, dynamic> data;
  final bool isLoading;

  const MarketCapView({
    super.key,
    required this.data,
    this.isLoading = false,
  });

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    final distribution = data['distribution'] as List<dynamic>? ?? [];

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [const Color(0xFF2E2E3E), const Color(0xFF252535)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(color: Colors.black45, blurRadius: 12, offset: const Offset(0, 6)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.donut_small, color: Colors.white, size: 24),
              SizedBox(width: 8),
              Text(
                'Market Cap Performance',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: distribution.map((item) {
              final category = item['category'] ?? 'Unknown';
              final change = (item['change'] ?? 0.0).toDouble();
              final name = item['name'] ?? '';
              
              final color = change >= 0 ? const Color(0xFF4CAF50) : const Color(0xFFEF5350);
              
              Color categoryColor;
              if (category.contains('Large')) {
                categoryColor = Colors.blue;
              } else if (category.contains('Mid')) {
                categoryColor = Colors.orange;
              } else {
                categoryColor = Colors.purple;
              }
              
              return Expanded(
                child: Container(
                  margin: const EdgeInsets.symmetric(horizontal: 4),
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.black26,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: categoryColor, width: 2),
                  ),
                  child: Column(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: categoryColor.withOpacity(0.2),
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          Icons.layers,
                          color: categoryColor,
                          size: 24,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        category,
                        style: TextStyle(
                          color: categoryColor,
                          fontWeight: FontWeight.bold,
                          fontSize: 13,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        name.replaceAll('NIFTY ', ''),
                        style: const TextStyle(
                          color: Colors.white60,
                          fontSize: 10,
                        ),
                        textAlign: TextAlign.center,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        '${change >= 0 ? '+' : ''}${change.toStringAsFixed(2)}%',
                        style: TextStyle(
                          color: color,
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }
}
