import 'package:flutter/material.dart';
import 'package:am_design_system/am_design_system.dart';
import '../../providers/market_provider.dart';


// Import actual screens/widgets
// Import actual screens/widgets
import 'package:am_market_ui/features/market_analysis/presentation/widgets/indices_performance_view_v2.dart';
import 'package:am_market_dev/features/developer/streamer_page.dart';
import 'package:am_market_ui/features/instrument/instrument_explorer_page.dart';
import 'package:am_market_ui/features/security/security_explorer_page.dart';
import 'package:am_market_dev/features/developer/price_test_page.dart';
import 'package:am_market_ui/features/etf/etf_explorer_page.dart';
// Analysis Page moved to shared or features
import 'package:am_market_ui/features/market_analysis/presentation/pages/analysis_page.dart';
import 'package:am_market_dev/features/developer/admin/historical_sync_page.dart';
import 'package:am_market_ui/shared/widgets/constituents_table.dart';
import 'package:am_market_ui/features/market_analysis/presentation/widgets/heatmap_view.dart';
import 'package:am_market_ui/features/market_analysis/presentation/widgets/heatmap_explorer_view.dart';
// MarketAnalyticsPage replaced by AnalysisPage


/// Market main content area - RESTORED FULL VERSION
class MarketMainContent extends StatefulWidget {
  const MarketMainContent({
    required this.provider,
    super.key,
  });

  final MarketProvider provider;

  @override
  State<MarketMainContent> createState() => _MarketMainContentState();
}

class _MarketMainContentState extends State<MarketMainContent> {
  int _viewMode = 0; // 0: Table, 1: Heatmap, 2: Analytics

  String _getValidIndexSymbol(String? index) {
    if (index == null ||
        index.isEmpty ||
        index == 'All Indices' ||
        index == 'Streamer' ||
        index == 'Instrument Explorer' ||
        index == 'Security Explorer' ||
        index == 'ETF Explorer' ||
        index == 'Price Test' || 
        index == 'Market Analysis' ||
        index == 'Admin Dashboard') { 
      return 'NIFTY 50';
    }
    return index;
  }

  @override
  Widget build(BuildContext context) {
    CommonLogger.methodEntry('build', tag: 'MarketMainContent');
    final provider = widget.provider;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final selectedIndex = provider.selectedIndex ?? 'All Indices';
    
    CommonLogger.debug('Building with selectedIndex: $selectedIndex', tag: 'MarketMainContent');

    return Container(
      decoration: AppGlassmorphismV2.techBackground(isDark: isDark),
      child: Stack(
        children: [
          // Main Content Stack
          Positioned.fill(
            child: IndexedStack(
              index: _determineIndex(selectedIndex),
              children: [
                // Index 0: Market Overview
                const IndicesPerformanceViewV2(),

                // Index 1: Streamer
                StreamerPage(),

                // Index 2: Instrument Explorer
                InstrumentExplorerPage(),

                // Index 3: Security Explorer
                SecurityExplorerPage(),

                // Index 4: Price Test
                PriceTestPage(),

                // Index 5: ETF Explorer
                const EtfExplorerPage(),

                // Index 6: Heatmap Explorer
                const HeatmapExplorerView(),

                // Index 7: Market Analysis (Global)
                const AnalysisPage(),

                // Index 8: Admin Dashboard
                const HistoricalSyncPage(),

                // Index 9: Specific Index View (Table/Heatmap/Analytics)
                _buildIndexDetailView(provider),

              ],
            ),
          ),

          // View Mode Toggler (only for specific index view)
          if (_determineIndex(selectedIndex) == 9)
            Positioned(
              top: 20,
              right: 20,
              child: _buildViewModeToggle(),
            ),
            
          // Global Loading Overlay
          if (provider.isLoading && selectedIndex == 'All Indices')
            const Center(child: CircularProgressIndicator(color: Color(0xFF06b6d4))),
        ],
      ),
    );
  }

  int _determineIndex(String selectedIndex) {
    switch (selectedIndex) {
      case 'All Indices': return 0;
      case 'Streamer': return 1;
      case 'Instrument Explorer': return 2;
      case 'Security Explorer': return 3;
      case 'Price Test': return 4;
      case 'ETF Explorer': return 5;
      case 'Heatmap Explorer': return 6;
      case 'Market Analysis': return 7;
      case 'Admin Dashboard': return 8;
      default: return 9; // Specific Index Detail
    }
  }

  Widget _buildIndexDetailView(MarketProvider provider) {
    if (provider.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    
    if (provider.error != null) {
      return Center(
        child: Text('Error: ${provider.error}', style: const TextStyle(color: Colors.redAccent)),
      );
    }

    switch (_viewMode) {
      case 0: return const ConstituentsTable();
      case 1: return const HeatmapView();
      case 2: return const AnalysisPage();
      default: return const ConstituentsTable();
    }
  }

  Widget _buildViewModeToggle() {
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: Colors.black.withOpacity(0.3),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.white.withOpacity(0.1)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _toggleButton(0, Icons.table_chart_rounded, 'Table'),
          _toggleButton(1, Icons.grid_view_rounded, 'Heatmap'),
          _toggleButton(2, Icons.analytics_rounded, 'Analytics'),
        ],
      ),
    );
  }

  Widget _toggleButton(int mode, IconData icon, String label) {
    final isSelected = _viewMode == mode;
    return GestureDetector(
      onTap: () => setState(() => _viewMode = mode),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected ? const Color(0xFF06b6d4).withOpacity(0.2) : Colors.transparent,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            Icon(icon, size: 16, color: isSelected ? const Color(0xFF06b6d4) : Colors.white60),
            const SizedBox(width: 4),
            Text(label, style: TextStyle(color: isSelected ? Colors.white : Colors.white60, fontSize: 12, fontWeight: isSelected ? FontWeight.bold : FontWeight.normal)),
          ],
        ),
      ),
    );
  }
}

