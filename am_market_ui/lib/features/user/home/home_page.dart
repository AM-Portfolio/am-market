import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:am_design_system/am_design_system.dart'; // Ensure common UI is imported
import '../providers/market_provider.dart';
import '../widgets/heatmap_view.dart';
import '../widgets/constituents_table.dart';
import '../widgets/indices_performance_view_v2.dart';
import '../screens/streamer_page.dart';
import '../screens/market_analytics_page.dart';
import '../screens/instrument_explorer_page.dart';
import '../screens/etf_explorer_page.dart';
import '../screens/admin/historical_sync_page.dart';
import '../screens/security_explorer_page.dart'; 
import '../screens/price_test_page.dart';
import '../features/analysis/analysis_page.dart';


class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _currentView = 0; // 0: Table, 1: Heatmap, 2: Analytics

  @override
  void initState() {
    super.initState();
    // Initialize market data on startup
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final provider = context.read<MarketProvider>();
      if (provider.availableIndices == null) {
        provider.loadIndices();
      }
    });
  }

  // Helper to filter out invalid index values
  String _getValidIndexSymbol(String? index) {
    if (index == null ||
        index.isEmpty ||
        index == 'All Indices' ||
        index == 'Streamer' ||
        index == 'Instrument Explorer' ||
        index == 'Instruments' ||
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
    // Determine which "tab/index" is selected in the provider
    final provider = context.watch<MarketProvider>();
    final selectedIndex = provider.selectedIndex;
    final bool isAllIndices = selectedIndex == "All Indices";
    final bool isStreamer = selectedIndex == "Streamer";
    final bool isInstruments = selectedIndex == "Instrument Explorer";
    final bool isSecurityExplorer = selectedIndex == "Security Explorer";
    final bool isEtfExplorer = selectedIndex == "ETF Explorer";
    final bool isPriceTest = selectedIndex == "Price Test";
    final bool isMarketAnalysis = selectedIndex == "Market Analysis";
    final bool isAdmin = selectedIndex == "Admin Dashboard";
    final isAnalytics = _currentView == 2;

    // Standardized Accent for Market Data
    const marketAccent = ModuleColors.market;

    // Build sidebar items
    final sidebarItems = [
      SecondarySidebarItem(
        title: 'All Indices',
        icon: Icons.dashboard_rounded,
        onTap: () => provider.selectIndex("All Indices"),
        accentColor: marketAccent,
        isSelected: isAllIndices,
      ),
      SecondarySidebarItem(
        title: 'Streamer',
        icon: Icons.waves,
        onTap: () => provider.selectIndex("Streamer"),
        accentColor: AppColors.accentPink,
        isSelected: isStreamer,
      ),
      SecondarySidebarItem(
        title: 'Instrument Explorer',
        icon: Icons.search,
        onTap: () => provider.selectIndex("Instrument Explorer"),
        accentColor: AppColors.accentBlue,
        isSelected: isInstruments,
      ),
      SecondarySidebarItem(
        title: 'Security Explorer',
        icon: Icons.security,
        onTap: () => provider.selectIndex("Security Explorer"),
        accentColor: AppColors.error,
        isSelected: isSecurityExplorer,
      ),
      SecondarySidebarItem(
        title: 'ETF Explorer',
        icon: Icons.dashboard_customize,
        onTap: () => provider.selectIndex("ETF Explorer"),
        accentColor: marketAccent,
        isSelected: isEtfExplorer,
      ),
      SecondarySidebarItem(
        title: 'Price Test',
        icon: Icons.price_check,
        onTap: () => provider.selectIndex("Price Test"),
        accentColor: AppColors.warning,
        isSelected: isPriceTest,
      ),
      SecondarySidebarItem(
        title: 'Market Analysis',
        icon: Icons.analytics,
        onTap: () => provider.selectIndex("Market Analysis"),
        accentColor: AppColors.success,
        isSelected: isMarketAnalysis,
      ),
    ];

    return UnifiedSidebarScaffold(
      title: 'Market Data',
      subtitle: 'Real-time insights',
      icon: Icons.trending_up_rounded,
      accentColor: marketAccent,
      items: sidebarItems,
      footer: _buildFooter(context, provider, isAdmin, marketAccent),
      body: AnimatedPageTransition(
        child: _buildContent(provider, isAllIndices, isStreamer, isInstruments, isSecurityExplorer, isEtfExplorer, isPriceTest, isMarketAnalysis, isAdmin, isAnalytics),
      ),
    );
  }

  Widget _buildFooter(BuildContext context, MarketProvider provider, bool isAdmin, Color accentColor) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // System Tools Header
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Text(
            'SYSTEM TOOLS',
            style: TextStyle(
              color: Colors.white.withValues(alpha: 0.5),
              fontSize: 11,
              fontWeight: FontWeight.bold,
              letterSpacing: 1.2,
            ),
          ),
        ),

        // Force Refresh Toggle
        GlassCard(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          borderRadius: 12,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(
                    provider.forceRefresh ? Icons.check_box : Icons.check_box_outline_blank,
                    color: provider.forceRefresh ? AppColors.success : Colors.white70,
                    size: 20,
                  ),
                  const SizedBox(width: 12),
                  const Text(
                    'Force Refresh',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
              Switch(
                value: provider.forceRefresh,
                onChanged: (val) => provider.toggleForceRefresh(val),
                activeColor: AppColors.success,
              ),
            ],
          ),
        ),

        const SizedBox(height: 8),

        // Refresh Cookies Button
        GlossyButton(
          text: 'Refresh Cookies',
          icon: Icons.cookie,
          onPressed: () async {
            CommonLogger.info("Refresh Cookies requested", tag: "AppSidebar");

            await provider.refreshCookies();
            if (context.mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(provider.error ?? "Cookies refreshed successfully!"),
                  backgroundColor: AppColors.success,
                ),
              );
            }
          },
          gradientColors: [
            accentColor,
            accentColor.withValues(alpha: 0.6),
          ],
          borderRadius: 10,
        ),

        const SizedBox(height: 8),

        // Admin Dashboard Button
        GlossyButton(
          text: 'Admin Dashboard',
          icon: Icons.admin_panel_settings,
          onPressed: () => provider.selectIndex("Admin Dashboard"),
          gradientColors: isAdmin
              ? const [AppColors.error, Color(0xFFD63031)]
              : [Colors.white.withOpacity(0.1), Colors.white.withOpacity(0.05)],
          borderRadius: 10,
        ),
      ],
    );
  }


   Widget _buildContent(MarketProvider provider, bool isAllIndices, bool isStreamer, bool isInstruments, bool isSecurityExplorer, bool isEtfExplorer, bool isPriceTest, bool isMarketAnalysis, bool isAdmin, bool isAnalytics) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Container(
      decoration: AppGlassmorphismV2.techBackground(isDark: isDark),
      child: LoadingWrapper(
        isLoading: provider.isLoading && (isAllIndices || (!isStreamer && !isInstruments && !isSecurityExplorer && !isPriceTest && !isEtfExplorer && !isMarketAnalysis && !isAdmin)),
        loadingWidget: _buildShimmerSkeleton(),
        child: IndexedStack(
          index: isAllIndices ? 0 : 
                 isStreamer ? 1 : 
                 isInstruments ? 2 : 
                 isSecurityExplorer ? 3 :
                 isPriceTest ? 4 : 
                 isEtfExplorer ? 5 : 
                 isMarketAnalysis ? 6 :
                 isAdmin ? 8 :
                 7, // Default (Index Details)
          children: [
             // Index 0: Market Overview (V2 Glassmorphic)
             const IndicesPerformanceViewV2(),

             // Index 1: Streamer
             StreamerPage(),

             // Index 2: Instruments
             InstrumentExplorerPage(),

             // Index 3: Security Explorer
             SecurityExplorerPage(),

             // Index 4: Price Test
             PriceTestPage(),

             // Index 5: ETF Explorer
             const EtfExplorerPage(),

             // Index 6: Market Analysis
             const AnalysisPage(),

             // Index 7: Default (Index View)
             _buildIndexView(provider, isAnalytics),
            
           // Index 8: Admin Dashboard
           HistoricalSyncPage(),
         ],
       ),
     ),
    );
  }

  Widget _buildIndexView(MarketProvider provider, bool isAnalytics) {
    if (provider.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (provider.error != null) {
      return Center(
        child: Text(
          'Error: ${provider.error}',
          style: const TextStyle(color: Colors.redAccent),
        ),
      );
    }
    if (isAnalytics) {
      return MarketAnalyticsPage(
        indexSymbol: _getValidIndexSymbol(provider.selectedIndex),
      );
    }
    return _currentView == 0
        ? const ConstituentsTable()
        : const HeatmapView();
  }

  Widget _buildShimmerSkeleton() {
    return ShimmerLoading(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header Skeleton
            Container(
              height: 60,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0),
                child: Row(
                  children: [
                     SkeletonBox(width: 40, height: 40, borderRadius: BorderRadius.circular(10)),
                     const SizedBox(width: 12),
                     Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: const [
                           SkeletonLine(width: 150, height: 16),
                           SizedBox(height: 8),
                           SkeletonLine(width: 100, height: 12),
                        ],
                     ),
                     const Spacer(),
                     SkeletonBox(width: 40, height: 40, borderRadius: BorderRadius.circular(20)),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
            
            // Selected Pills Skeleton
            Container(
               height: 50,
               decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(0),
               ),
               child: Padding(
                 padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12),
                 child: Row(
                    children: [
                       SkeletonBox(width: 80, height: 26, borderRadius: BorderRadius.circular(20)),
                       const SizedBox(width: 8),
                       SkeletonBox(width: 80, height: 26, borderRadius: BorderRadius.circular(20)),
                    ],
                 ),
               ),
            ),
     
            const SizedBox(height: 20),

            // Chart Skeleton
            SkeletonBox(height: 400, borderRadius: BorderRadius.circular(12)),
            
            const SizedBox(height: 24),

            // Index Selector Skeleton
            Container(
              // Removed fixed height to prevent overflow
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                 crossAxisAlignment: CrossAxisAlignment.start,
                 children: [
                    const SkeletonLine(width: 200, height: 18),
                    const SizedBox(height: 16),
                    Wrap(
                       spacing: 8,
                       runSpacing: 8,
                       children: List.generate(8, (index) => SkeletonBox(width: 100, height: 32, borderRadius: BorderRadius.circular(8))),
                    )
                 ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
