import 'package:flutter/material.dart';
import 'package:am_market_common/providers/market_provider.dart';
import 'package:am_market_ui/features/market_analysis/presentation/pages/analysis_page.dart';
import 'package:am_market_ui/shared/widgets/constituents_table.dart';
import 'package:am_market_ui/features/market_analysis/presentation/widgets/heatmap_view.dart';

class MarketIndexDetailView extends StatefulWidget {
  const MarketIndexDetailView({
    required this.provider,
    required this.indexSymbol,
    super.key,
  });

  final MarketProvider provider;
  final String indexSymbol;

  @override
  State<MarketIndexDetailView> createState() => _MarketIndexDetailViewState();
}

class _MarketIndexDetailViewState extends State<MarketIndexDetailView> {
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
  Widget build(BuildContext context) => Stack(
    children: [
      Positioned.fill(child: _buildContent()),
      Positioned(top: 20, right: 20, child: _buildViewModeToggle()),
    ],
  );

  Widget _buildContent() {
    if (widget.provider.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (widget.provider.error != null) {
      return Center(
        child: Text(
          'Error: ${widget.provider.error}',
          style: const TextStyle(color: Colors.redAccent),
        ),
      );
    }

    switch (_viewMode) {
      case 0:
        return const ConstituentsTable();
      case 1:
        return const HeatmapView();
      case 2:
        return const AnalysisPage();
      default:
        return const ConstituentsTable();
    }
  }

  Widget _buildViewModeToggle() => Container(
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

  Widget _toggleButton(int mode, IconData icon, String label) {
    final isSelected = _viewMode == mode;
    return GestureDetector(
      onTap: () => setState(() => _viewMode = mode),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected
              ? const Color(0xFF06b6d4).withOpacity(0.2)
              : Colors.transparent,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            Icon(
              icon,
              size: 16,
              color: isSelected ? const Color(0xFF06b6d4) : Colors.white60,
            ),
            const SizedBox(width: 4),
            Text(
              label,
              style: TextStyle(
                color: isSelected ? Colors.white : Colors.white60,
                fontSize: 12,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
