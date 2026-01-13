import 'package:am_design_system/am_design_system.dart';
import 'package:flutter/material.dart';
import 'package:am_market_ui/features/user/analysis/analysis_page.dart';
import 'package:am_market_ui/providers/market_provider.dart';
import 'package:am_market_ui/features/developer/admin/historical_sync_page.dart';
import 'package:am_market_ui/features/developer/admin_dashboard_page.dart';
import 'package:am_market_ui/features/user/etf/etf_explorer_page.dart';
import 'package:am_market_ui/features/user/instrument/instrument_explorer_page.dart';
import 'package:am_market_ui/features/developer/price_test_page.dart';
import 'package:am_market_ui/features/user/security/security_explorer_page.dart';
import 'package:am_market_ui/features/developer/streamer_page.dart';
import 'package:am_market_ui/widgets/indices_performance_view_v2.dart';
import 'package:provider/provider.dart';

import 'package:am_common/core/utils/logger.dart';
import 'package:am_market_ui/widgets/market_index_detail_view.dart';
import 'package:am_market_ui/features/developer/screens/developer_dashboard.dart';
import 'package:am_market_ui/providers/view_mode_provider.dart';
import 'package:am_market_ui/widgets/mode_toggle_widget.dart';
import 'package:am_market_ui/features/user/market/presentation/pages/user_dashboard_page.dart';
import 'package:am_market_ui/features/user/market/presentation/widgets/heatmap_explorer_view.dart';
import 'package:am_market_ui/widgets/vertical_scroll_navigator.dart';

/// Market feature page with Swipe Navigation
class MarketPage extends StatelessWidget {
  const MarketPage({required this.userId, super.key, this.onBack});

  final String userId;
  final VoidCallback? onBack;

  @override
  Widget build(BuildContext context) {
    CommonLogger.methodEntry('build', tag: 'MarketPage');

    return MultiProvider(
      providers: [
        ChangeNotifierProvider(
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
        ),
        ChangeNotifierProvider(
          create: (_) => ViewModeProvider(),
        ),
      ],
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
      items: _buildNavigationItems(
        context.read<MarketProvider>(),
        context.read<ViewModeProvider>(),
      ),
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

  void _navigateToNext() {
    // Only navigate if not at the last item
    if (_swipeController.currentIndex < _swipeController.items.length - 1) {
      _swipeController.navigateTo(_swipeController.currentIndex + 1);
    }
  }

  void _navigateToPrev() {
    // Only navigate if not at the first item
    if (_swipeController.currentIndex > 0) {
      _swipeController.navigateTo(_swipeController.currentIndex - 1);
    }
  }

  Widget _wrapPage(Widget page) {
    return VerticalScrollNavigator(
      child: page,
      onNextPage: _navigateToNext,
      onPreviousPage: _navigateToPrev,
    );
  }

  @override
  Widget build(BuildContext context) => Consumer2<MarketProvider, ViewModeProvider>(
    builder: (context, provider, viewModeProvider, _) {
      // Update controller items when provider updates (e.g. indices loaded)
      final newItems = _buildNavigationItems(provider, viewModeProvider);
      if (_hasItemsChanged(newItems)) {
        // Defer update to avoid build cycle
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) {
            _swipeController.updateItems(newItems);
          }
        });
      }

      return UnifiedSidebarScaffold(
        module: ModuleType.market,
        onBackToGlobal: widget.onBack,
        body: SwipeablePageView(
          key: const PageStorageKey('market_page_info'), // Maintain state across layout rebuilds
          scrollDirection: Axis.vertical,
          controller: _swipeController,
          showIndicator: !provider.isLoading,
        ),
        sections: _buildSidebarSections(provider, viewModeProvider),
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

  List<SecondarySidebarSection> _buildSidebarSections(
    MarketProvider provider,
    ViewModeProvider viewModeProvider,
  ) {
    // If User mode, show simplified navigation
    if (viewModeProvider.isUserMode) {
      return _buildUserModeSections(provider);
    }
    
    // Developer mode - show all sections (existing behavior)
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
    final developerIndex = adminIndex + 1;

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

    final developerItem = SecondarySidebarItem(
      title: 'Developer Dashboard',
      icon: Icons.developer_mode_rounded,
      isSelected: currentIndex == developerIndex,
      accentColor: Colors.deepPurple,
      onTap: () {
        _swipeController.navigateTo(developerIndex.toInt());
        provider.selectIndex('Developer Dashboard');
      },
    );

    // Add mode toggle as first section (using section's customWidget)
    final modeToggleSection = SecondarySidebarSection(
      title: '',
      items: [], // Empty items since we're using customWidget
      customWidget: const ModeToggleWidget(),
    );

    return [
      modeToggleSection,
      SecondarySidebarSection(title: 'Data', items: mainItems),
      if (indexItems.isNotEmpty)
        SecondarySidebarSection(title: 'Major Ind ices', items: indexItems),
      SecondarySidebarSection(title: 'System Tools', items: [adminItem, developerItem]),
    ];
  }

  // User Mode - Simplified Navigation (Dashboard, Overview, Heatmap)
  List<SecondarySidebarSection> _buildUserModeSections(MarketProvider provider) {
    const accentColor = ModuleColors.market;
    final currentIndex = _swipeController.currentIndex;

    final userItems = [
      _createSidebarItem(0, 'Dashboard', Icons.home_rounded, 'Overview'),
      _createSidebarItem(1, 'Market Analysis', Icons.analytics_rounded, 'Detailed charts'),
    ];

    // Mode toggle at the top
    final modeToggleSection = SecondarySidebarSection(
      title: '',
      items: [],
      customWidget: const ModeToggleWidget(),
    );

    return [
      modeToggleSection,
      SecondarySidebarSection(title: 'Navigation', items: userItems),
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

  List<NavigationItem> _buildNavigationItems(
    MarketProvider provider,
    ViewModeProvider viewModeProvider,
  ) {
    // If User mode, show only 3 pages: Dashboard, Market Analysis, Heatmap
    if (viewModeProvider.isUserMode) {
      return _buildUserModeNavigationItems(provider);
    }
    
    // Developer mode - show all items
    const accentColor = ModuleColors.market;

    final items = [
      NavigationItem(
        title: 'All Indices',
        subtitle: 'Market Overview',
        icon: Icons.dashboard_rounded,
        page: _wrapPage(const IndicesPerformanceViewV2()),
        accentColor: accentColor,
      ),
      NavigationItem(
        title: 'Streamer',
        subtitle: 'Real-time data',
        icon: Icons.waves_rounded,
        page: _wrapPage(const StreamerPage()),
        accentColor: accentColor,
      ),
      NavigationItem(
        title: 'Instrument Explorer',
        subtitle: 'Search instruments',
        icon: Icons.manage_search_rounded,
        page: _wrapPage(const InstrumentExplorerPage()),
        accentColor: accentColor,
      ),
      NavigationItem(
        title: 'Security Explorer',
        subtitle: 'Security details',
        icon: Icons.security_rounded,
        page: _wrapPage(const SecurityExplorerPage()),
        accentColor: accentColor,
      ),
      NavigationItem(
        title: 'ETF Explorer',
        subtitle: 'ETF insights',
        icon: Icons.dashboard_customize_rounded,
        page: _wrapPage(const EtfExplorerPage()),
        accentColor: accentColor,
      ),
      NavigationItem(
        title: 'Price Test',
        subtitle: 'Price validation',
        icon: Icons.price_check_rounded,
        page: _wrapPage(const PriceTestPage()),
        accentColor: accentColor,
      ),
      NavigationItem(
        title: 'Market Analysis',
        subtitle: 'Detailed charts',
        icon: Icons.analytics_rounded,
        page: _wrapPage(const AnalysisPage()),
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
            page: _wrapPage(MarketIndexDetailView(
              provider: provider,
              indexSymbol: indexName,
            )),
            accentColor: accentColor,
          ),
        );
      }
    }

    // Admin
    items.add(
      NavigationItem(
        title: 'Admin Dashboard',
        subtitle: 'System Tools',
        icon: Icons.admin_panel_settings_rounded,
        page: _wrapPage(const AdminDashboardPage()),
        accentColor: Color(0xFFFF6B6B),
      ),
    );

    items.add(
      NavigationItem(
        title: 'Developer Dashboard',
        subtitle: 'Dev Tools & Scheduler',
        icon: Icons.developer_mode_rounded,
        page: _wrapPage(const DeveloperDashboard()),
        accentColor: Colors.deepPurple,
      ),
    );

    return items;
  }

  List<NavigationItem> _buildUserModeNavigationItems(MarketProvider provider) {
    const accentColor = ModuleColors.market;

    return [
      NavigationItem(
        title: 'Dashboard',
        subtitle: 'Overview',
        icon: Icons.home_rounded,
        page: _wrapPage(const UserDashboardPage()), // User Dashboard with cards + chart
        accentColor: accentColor,
      ),
      NavigationItem(
        title: 'Market Analysis',
        subtitle: 'Heatmap & Details',
        icon: Icons.analytics_rounded,
        page: _wrapPage(const HeatmapExplorerView()), // Was AnalysisPage(), now consolidated
        accentColor: accentColor,
      ),
      // Heatmap Explorer removed as it's now Market Analysis
    ];
  }
}
