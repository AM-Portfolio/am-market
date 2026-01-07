import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:am_design_system/am_design_system.dart';
import '../providers/market_provider.dart';
import '../models/market_data.dart';

/// V2 Glassmorphic Indices Performance View with Architecture Cards
class IndicesPerformanceViewV2 extends StatelessWidget {
  const IndicesPerformanceViewV2({Key? key}) : super(key: key);

  Color _getColorSchemeForChange(double pChange) {
    if (pChange >= 2.0) return const Color(0xFF00B894); // Strong Green
    if (pChange >= 0) return const Color(0xFF00D2D3); // Cyan
    if (pChange >= -2.0) return const Color(0xFFFF9F43); // Orange
    return const Color(0xFFFF6B6B); // Red
  }

  String _getColorSchemeName(double pChange) {
    if (pChange >= 2.0) return 'success';
    if (pChange >= 0) return 'info';
    if (pChange >= -2.0) return 'accent';
    return 'neutral';
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<MarketProvider>(
      builder: (context, provider, child) {
        if (provider.isLoading) {
          return const Center(child: CircularProgressIndicator());
        }

        if (provider.error != null) {
          return Center(
            child: Text(
              'Error: ${provider.error}',
              style: const TextStyle(color: Colors.red),
            ),
          );
        }

        if (provider.allIndicesData.isEmpty) {
          return const Center(
            child: Text(
              'No data loaded',
              style: TextStyle(color: Colors.white70),
            ),
          );
        }

        final allIndices = List<StockIndicesMarketData>.from(provider.allIndicesData);
        allIndices.sort((a, b) => b.pChange.compareTo(a.pChange));
        final isDark = Theme.of(context).brightness == Brightness.dark;

        return Container(
          decoration: AppGlassmorphismV2.techBackground(isDark: isDark),
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header
                InfoLayerCard(
                  title: 'Market Overview',
                  subtitle: 'Real-time indices performance',
                  icon: Icons.dashboard,
                  colorScheme: 'primary',
                ),

                const SizedBox(height: 32),

                // Top Performing Indices
                Text(
                  'TOP PERFORMERS',
                  style: TextStyle(
                    color: isDark ? Colors.white38 : Colors.black45,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 2,
                  ),
                ),
                const SizedBox(height: 16),

                GridView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                    maxCrossAxisExtent: 350,
                    childAspectRatio: 1.4,
                    crossAxisSpacing: 20,
                    mainAxisSpacing: 20,
                  ),
                  itemCount: allIndices.take(6).length,
                  itemBuilder: (context, index) {
                    final data = allIndices[index];
                    // Cycle schemes for visual variety in White Mode
                    final schemes = ['primary', 'accent', 'neutral', 'info', 'success'];
                    final scheme = schemes[index % schemes.length];
                    final colors = AppGlassmorphismV2.colorSchemes[scheme]!;
                    
                    return MetricCard(
                      label: data.indexSymbol,
                      value: data.lastPrice.toStringAsFixed(2),
                      icon: data.pChange >= 0 ? Icons.trending_up : Icons.trending_down,
                      accentColor: isDark ? _getColorSchemeForChange(data.pChange) : colors[0],
                      trailing: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: (isDark ? _getColorSchemeForChange(data.pChange) : colors[0]).withOpacity(0.2),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          '${data.pChange >= 0 ? '+' : ''}${data.pChange.toStringAsFixed(2)}%',
                          style: TextStyle(
                            color: isDark ? _getColorSchemeForChange(data.pChange) : colors[0],
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    );
                  },
                ),

                const SizedBox(height: 40),

                // All Indices
                Text(
                  'ALL INDICES',
                  style: TextStyle(
                    color: isDark ? Colors.white38 : Colors.black45,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 2,
                  ),
                ),
                const SizedBox(height: 16),

                GridView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                    maxCrossAxisExtent: 300,
                    childAspectRatio: 1.5,
                    crossAxisSpacing: 16,
                    mainAxisSpacing: 16,
                  ),
                  itemCount: allIndices.length,
                  itemBuilder: (context, index) {
                    final data = allIndices[index];
                    final isPositive = data.pChange >= 0;
                    
                    // Cycle schemes for visual variety in White Mode
                    final schemes = ['primary', 'accent', 'neutral', 'info', 'success'];
                    final scheme = schemes[index % schemes.length];
                    final colors = AppGlassmorphismV2.colorSchemes[scheme]!;

                    return GlassCard(
                      padding: const EdgeInsets.all(16),
                      // Use scheme for V2 styling (pastel fill in white mode)
                      colorScheme: scheme, 
                      onTap: () => provider.selectIndex(data.indexSymbol),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Text(
                                  data.indexSymbol,
                                  style: TextStyle(
                                    color: isDark ? Colors.white : Colors.black87,
                                    fontSize: 13,
                                    fontWeight: FontWeight.bold,
                                  ),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              Icon(
                                isPositive ? Icons.arrow_upward : Icons.arrow_downward,
                                color: isDark ? _getColorSchemeForChange(data.pChange) : colors[0],
                                size: 16,
                              ),
                            ],
                          ),
                          Text(
                            data.lastPrice.toStringAsFixed(2),
                            style: TextStyle(
                              color: isDark ? Colors.white : Colors.black87,
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          Text(
                            '${isPositive ? '+' : ''}${data.pChange.toStringAsFixed(2)}%',
                            style: TextStyle(
                              color: isDark ? _getColorSchemeForChange(data.pChange) : colors[0],
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
