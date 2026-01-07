import 'package:flutter/material.dart';

class SectorPerformanceView extends StatelessWidget {
  final List<Map<String, dynamic>> sectors;
  final bool isLoading;

  const SectorPerformanceView({
    super.key,
    required this.sectors,
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
              Icon(Icons.pie_chart, color: Colors.purpleAccent, size: 24),
              SizedBox(width: 8),
              Text(
                'Sector Performance',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          SizedBox(
            height: 250,
            child: ListView.builder(
              itemCount: sectors.length,
              itemBuilder: (context, index) {
                final sector = sectors[index];
                final sectorName = sector['sector'] ?? 'Unknown';
                final change = (sector['change'] ?? 0.0).toDouble();
                final stockCount = sector['stockCount'] ?? 0;
                
                final color = change >= 0 ? Colors.green : Colors.red;
                
                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: Row(
                    children: [
                      Expanded(
                        flex: 2,
                        child: Text(
                          sectorName,
                          style: const TextStyle(color: Colors.black87, fontSize: 13, fontWeight: FontWeight.bold),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      Expanded(
                        flex: 3,
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(4),
                          child: LinearProgressIndicator(
                            value: (change.abs() / 5.0).clamp(0.0, 1.0),
                            backgroundColor: Colors.grey.shade100,
                            valueColor: AlwaysStoppedAnimation<Color>(color),
                            minHeight: 8,
                          ),
                        ),
                      ),
                      const SizedBox(width: 12),
                      SizedBox(
                        width: 70,
                        child: Text(
                          '${change >= 0 ? '+' : ''}${change.toStringAsFixed(2)}%',
                          style: TextStyle(
                            color: color,
                            fontWeight: FontWeight.bold,
                            fontSize: 13,
                          ),
                          textAlign: TextAlign.right,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        '($stockCount)',
                        style: TextStyle(color: Colors.grey.shade500, fontSize: 11),
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
