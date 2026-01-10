import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:am_market_ui/providers/market_provider.dart';
import 'package:am_market_ui/widgets/index_cards_carousel.dart';
import 'package:am_market_ui/widgets/indices_performance_view_v2.dart';

/// User Dashboard page with horizontal index cards and chart view
class UserDashboardPage extends StatelessWidget {
  const UserDashboardPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<MarketProvider>(
      builder: (context, provider, _) {
        // Loading state
        if (provider.isLoading && provider.allIndicesData.isEmpty) {
          return const Center(
            child: CircularProgressIndicator(
              color: Color(0xFF00D1FF),
            ),
          );
        }

        // Error state
        if (provider.error != null && provider.allIndicesData.isEmpty) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  Icons.error_outline,
                  size: 48,
                  color: Colors.white.withOpacity(0.5),
                ),
                const SizedBox(height: 16),
                Text(
                  'Error loading data',
                  style: TextStyle(
                    color: Colors.white.withOpacity(0.8),
                    fontSize: 16,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  provider.error ?? '',
                  style: TextStyle(
                    color: Colors.white.withOpacity(0.5),
                    fontSize: 12,
                  ),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          );
        }

        // Empty state
        if (provider.allIndicesData.isEmpty) {
          return Center(
            child: Text(
              'No data available',
              style: TextStyle(
                color: Colors.white.withOpacity(0.5),
                fontSize: 14,
              ),
            ),
          );
        }

        // Main content
        return Column(
          children: [
            // Horizontal scrolling index cards
            IndexCardsCarousel(indices: provider.allIndicesData),
            
            // Divider
            Container(
              height: 1,
              margin: const EdgeInsets.symmetric(horizontal: 16),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    Colors.transparent,
                    Colors.white.withOpacity(0.1),
                    Colors.transparent,
                  ],
                ),
              ),
            ),
            
            // Chart view below
            const Expanded(
              child: IndicesPerformanceViewV2(),
            ),
          ],
        );
      },
    );
  }
}
