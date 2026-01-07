import 'package:am_design_system/am_design_system.dart';
import 'package:flutter/material.dart';
import 'package:am_market_ui/features/analysis/analysis_page.dart';
import 'package:am_market_ui/providers/market_provider.dart';
import 'package:am_market_ui/screens/admin/historical_sync_page.dart';
import 'package:am_market_ui/screens/etf_explorer_page.dart';
import 'package:am_market_ui/screens/instrument_explorer_page.dart';
import 'package:am_market_ui/screens/price_test_page.dart';
import 'package:am_market_ui/screens/security_explorer_page.dart';
import 'package:am_market_ui/screens/streamer_page.dart';
import 'package:am_market_ui/widgets/indices_performance_view_v2.dart';
import 'package:provider/provider.dart';

import 'package:am_common/core/utils/logger.dart';
import 'package:am_market_ui/widgets/market_index_detail_view.dart';

/// Market feature page with Swipe Navigation
class MarketPage extends StatelessWidget {
  const MarketPage({required this.userId, super.key, this.onBack});

  final String userId;
  final VoidCallback? onBack;

  @override
  Widget build(BuildContext context) {
    CommonLogger.methodEntry('build', tag: 'MarketPage');

    return ChangeNotifierProvider(
      create: (_) {
        CommonLogger.info(
          'Initializing MarketProvider for MarketPage',
          tag: 'MarketPage',
        );
        final provider = MarketProvider();
        // Trigger initial load
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (provider.availableIndices == null) {
            provider.loadIndices();
          }
        });
        return provider;
      },
      child: MarketContent(userId: userId, onBack: onBack),
    );
  }
}

class MarketContent extends StatefulWidget {
  const MarketContent({required this.userId, this.onBack, super.key});

  final String userId;
  final VoidCallback? onBack;

  @override
  State<MarketContent> createState() => _MarketContentState();
}

class _MarketContentState extends State<MarketContent> {
  late SwipeNavigationController _swipeController;

  @override
  void initState() {
    super.initState();
    _initializeSwipeController();
  }

  void _initializeSwipeController() {
    _swipeController = SwipeNavigationController(
      items: _buildNavigationItems(context.read<MarketProvider>()),
    );

    _swipeController.addListener(() {
      if (!mounted) return;
      setState(() {});

      // Sync provider with new selection
      final currentTitle = _swipeController.currentItem.title;
      final provider = context.read<MarketProvider>();
      if (provider.selectedIndex != currentTitle) {
        provider.selectIndex(currentTitle);
      }
    });
  }

  @override
  Widget build(BuildContext context) => Consumer<MarketProvider>(
    builder: (context, provider, _) {
      // Update controller items when provider updates (e.g. indices loaded)
      // We use a post-frame callback to avoid updating during build if possible,
      // or just update directly if it's safe.
      // Since updateItems calls notifyListeners, doing it in build might error.
      // Better to check if items changed.

      final newItems = _buildNavigationItems(provider);
      if (_hasItemsChanged(newItems)) {
        // Defer update to avoid build cycle
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) {
            _swipeController.updateItems(newItems);
          }
        });
      }

      // Also sync simple selectedIndex from provider if it was changed externally?
      // Actually, we want the controller to drive the provider if anything.
      // But for sidebar selection, we use controller.currentIndex.

      return UnifiedSidebarScaffold(
        module: ModuleType.market,
        onBackToGlobal: widget.onBack,
        body: SwipeablePageView(
          controller: _swipeController,
          showIndicator: !provider.isLoading,
        ),
        sections: _buildSidebarSections(provider),
      );
    },
  );

  bool _hasItemsChanged(List<NavigationItem> newItems) {
    if (_swipeController.items.length != newItems.length) return true;
    // Simple check on titles or structure
    for (var i = 0; i < newItems.length; i++) {
      if (_swipeController.items[i].title != newItems[i].title) return true;
    }
    return false;
  }

  List<SecondarySidebarSection> _buildSidebarSections(MarketProvider provider) {
    // Map controller items back to sections
    // Indices:
    // 0: All Indices
    // 1: Streamer
    // 2: Instrument Explorer
    // 3: Security Explorer
    // 4: ETF Explorer
    // 5: Price Test
    // 6: Market Analysis
    // 7..(7+N): Dynamic Indices
    // Last: Admin

    const accentColor = ModuleColors.market;
    final currentIndex = _swipeController.currentIndex;

    final mainItems = [
      _createSidebarItem(
        0,
        'All Indices',
        Icons.dashboard_rounded,
        'Market Overview',
      ),
      _createSidebarItem(1, 'Streamer', Icons.waves_rounded, 'Real-time data'),
      _createSidebarItem(
        2,
        'Instrument Explorer',
        Icons.manage_search_rounded,
        'Search instruments',
      ),
      _createSidebarItem(
        3,
        'Security Explorer',
        Icons.security_rounded,
        'Security details',
      ),
      _createSidebarItem(
        4,
        'ETF Explorer',
        Icons.dashboard_customize_rounded,
        'ETF insights',
      ),
      _createSidebarItem(
        5,
        'Price Test',
        Icons.price_check_rounded,
        'Price validation',
      ),
      _createSidebarItem(
        6,
        'Market Analysis',
        Icons.analytics_rounded,
        'Detailed charts',
      ),
    ];

    // Dynamic Indices
    final dynamicIndicesCount =
        provider.availableIndices?.broad.take(5).length ?? 0;
    final indexItems = <SecondarySidebarItem>[];
    if (provider.availableIndices != null) {
      var baseIndex = 7;
      for (final indexName in provider.availableIndices!.broad.take(5)) {
        final i = baseIndex;
        indexItems.add(
          SecondarySidebarItem(
            title: indexName,
            icon: Icons.trending_up_rounded,
            subtitle: 'Live Index Data',
            isSelected: currentIndex == i,
            accentColor: accentColor,
            onTap: () {
              _swipeController.navigateTo(i);
              provider.selectIndex(
                indexName,
              ); // Keep provider in sync if needed
            },
          ),
        );
        baseIndex++;
      }
    }

    final adminIndex = 7 + dynamicIndicesCount;
    final adminItem = SecondarySidebarItem(
      title: 'Admin Dashboard',
      icon: Icons.admin_panel_settings_rounded,
      isSelected: currentIndex == adminIndex,
      accentColor: const Color(0xFFFF6B6B),
      onTap: () {
        _swipeController.navigateTo(adminIndex.toInt());
        provider.selectIndex('Admin Dashboard');
      },
    );

    return [
      SecondarySidebarSection(title: 'Data', items: mainItems),
      if (indexItems.isNotEmpty)
        SecondarySidebarSection(title: 'Major Indices', items: indexItems),
      SecondarySidebarSection(title: 'System Tools', items: [adminItem]),
    ];
  }

  SecondarySidebarItem _createSidebarItem(
    int index,
    String title,
    IconData icon,
    String subtitle,
  ) => SecondarySidebarItem(
    title: title,
    icon: icon,
    subtitle: subtitle,
    isSelected: _swipeController.currentIndex == index,
    accentColor: ModuleColors.market,
    onTap: () {
      _swipeController.navigateTo(index);
      context.read<MarketProvider>().selectIndex(title); // Sync provider
    },
  );

  List<NavigationItem> _buildNavigationItems(MarketProvider provider) {
    const accentColor = ModuleColors.market;

    final items = [
      const NavigationItem(
        title: 'All Indices',
        subtitle: 'Market Overview',
        icon: Icons.dashboard_rounded,
        page: IndicesPerformanceViewV2(),
        accentColor: accentColor,
      ),
      const NavigationItem(
        title: 'Streamer',
        subtitle: 'Real-time data',
        icon: Icons.waves_rounded,
        page: StreamerPage(),
        accentColor: accentColor,
      ),
      const NavigationItem(
        title: 'Instrument Explorer',
        subtitle: 'Search instruments',
        icon: Icons.manage_search_rounded,
        page: InstrumentExplorerPage(),
        accentColor: accentColor,
      ),
      const NavigationItem(
        title: 'Security Explorer',
        subtitle: 'Security details',
        icon: Icons.security_rounded,
        page: SecurityExplorerPage(),
        accentColor: accentColor,
      ),
      const NavigationItem(
        title: 'ETF Explorer',
        subtitle: 'ETF insights',
        icon: Icons.dashboard_customize_rounded,
        page: EtfExplorerPage(),
        accentColor: accentColor,
      ),
      const NavigationItem(
        title: 'Price Test',
        subtitle: 'Price validation',
        icon: Icons.price_check_rounded,
        page: PriceTestPage(),
        accentColor: accentColor,
      ),
      const NavigationItem(
        title: 'Market Analysis',
        subtitle: 'Detailed charts',
        icon: Icons.analytics_rounded,
        page: AnalysisPage(),
        accentColor: accentColor,
      ),
    ];

    // Dynamic Indices
    if (provider.availableIndices != null) {
      for (final indexName in provider.availableIndices!.broad.take(5)) {
        items.add(
          NavigationItem(
            title: indexName,
            subtitle: 'Live Index Data',
            icon: Icons.trending_up_rounded,
            page: MarketIndexDetailView(
              provider: provider,
              indexSymbol: indexName,
            ),
            accentColor: accentColor,
          ),
        );
      }
    }

    // Admin
    items.add(
      const NavigationItem(
        title: 'Admin Dashboard',
        subtitle: 'System Tools',
        icon: Icons.admin_panel_settings_rounded,
        page: HistoricalSyncPage(),
        accentColor: Color(0xFFFF6B6B),
      ),
    );

    return items;
  }
}
