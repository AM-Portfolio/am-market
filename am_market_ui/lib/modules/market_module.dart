import 'package:flutter/material.dart';
import 'package:am_design_system/am_design_system.dart';
import 'package:provider/provider.dart';
import 'widgets/market_sidebar_content.dart';
import 'widgets/market_main_content.dart';
import '../providers/market_provider.dart';

/// Market Data Module
/// Implements IModule interface for embeddability
class MarketModule implements IModule {
  /// Constructor
  /// @param includeGlobalSidebar - Whether to show global navigation sidebar (default: false for embedded mode)
  const MarketModule({
    this.includeGlobalSidebar = false,
  });

  /// Whether to include the global sidebar (for standalone mode)
  final bool includeGlobalSidebar;

  @override
  String get moduleId => 'market';

  @override
  ModuleConfig get config => ModuleConfig(
        moduleId: moduleId,
        title: 'Market Data',
        subtitle: 'Real-time market insights',
        icon: Icons.trending_up_rounded,
        accentColor: ModuleColors.market, // Cyan
        showInNavigation: true,
        order: 3,
      );

  @override
  bool get requiresAuth => false; // Market data can be public

  @override
  bool get isEnabled => true;

  @override
  Future<void> configure(ModuleContext moduleContext) async {
    // Initialize any async resources here
    // For now, just a placeholder
    await Future.delayed(const Duration(milliseconds: 100));
  }

  @override
  void dispose() {
    // Cleanup resources iAre youf needed
  }

  @override
  Widget build(BuildContext context, ModuleContext moduleContext) {
    return ChangeNotifierProvider(
      create: (_) => MarketProvider(),
      child: Consumer<MarketProvider>(
        builder: (context, provider, _) {
          final moduleContent = Scaffold(
            backgroundColor: Colors.transparent,
            body: Row(
              children: [

                // Market Sidebar using standardized widget
                MarketSidebar(provider: provider),
                
                // Main Content Area
                Expanded(
                  child: MarketMainContent(provider: provider),
                ),
              ],
            ),
          );

          // If includeGlobalSidebar is true, wrap with GlobalSidebar
          if (includeGlobalSidebar) {
            return Row(
              children: [
                // Global Navigation Sidebar
                GlobalSidebar(
                  activeNavItem: 'Market', // Mark Market as active
                  onNavigate: (route) {
                    // Navigation handled by parent app's router
                    // For now, just print (will be implemented in integration)
                    print('Navigate to: $route');
                  },
                  navItems: [
                    GlobalNavigationItem(
                      title: 'Dashboard',
                      icon: Icons.dashboard_rounded,
                      route: '/dashboard',
                    ),
                    GlobalNavigationItem(
                      title: 'Portfolio',
                      icon: Icons.pie_chart_rounded,
                      route: '/portfolio',
                    ),
                    GlobalNavigationItem(
                      title: 'Trade',
                      icon: Icons.swap_horiz_rounded,
                      route: '/trade',
                    ),
                    GlobalNavigationItem(
                      title: 'Market',
                      icon: Icons.trending_up_rounded,
                      route: '/market',
                    ),
                  ],
                  isDarkMode: Theme.of(context).brightness == Brightness.dark,
                  onThemeToggle: () {
                    // Theme toggle handled by parent app
                  },
                ),
                // Module content
                Expanded(child: moduleContent),
              ],
            );
          }

          // Return module content without GlobalSidebar (embedded mode)
          return moduleContent;
        },
      ),
    );
  }

  @override
  Map<String, WidgetBuilder> get routes => {};
}
