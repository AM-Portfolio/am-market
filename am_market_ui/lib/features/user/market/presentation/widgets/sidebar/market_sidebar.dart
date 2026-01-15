
import 'package:flutter/material.dart';
import 'package:am_design_system/shared/widgets/navigation/secondary_sidebar.dart';
import 'package:am_design_system/core/theme/app_colors.dart';
import 'package:am_market_common/providers/market_provider.dart';

/// Market-specific sidebar using shared SecondarySidebar component
class MarketSidebar extends StatelessWidget {
  const MarketSidebar({
    required this.provider,
    super.key,
  });

  final MarketProvider provider;

  @override
  Widget build(BuildContext context) {
    final selectedIndex = provider.selectedIndex ?? 'All Indices';
    // Using Cyan for Market context
    const marketAccent = Color(0xFF00D1FF); 
    final isDark = Theme.of(context).brightness == Brightness.dark;

    // Define navigation items
    final indicesItems = [
      SecondarySidebarItem(
        title: 'All Indices',
        icon: Icons.dashboard_rounded,
        accentColor: marketAccent,
        isSelected: selectedIndex == 'All Indices',
        onTap: () => provider.selectIndex('All Indices'),
      ),
      SecondarySidebarItem(
        title: 'Major Indices',
        icon: Icons.trending_up_rounded,
        accentColor: marketAccent,
        isSelected: selectedIndex == 'Major Indices',
        onTap: () => provider.selectIndex('Major Indices'),
         // Subtitle/Trailing can be added if needed
      ),
       SecondarySidebarItem(
        title: 'Streamer',
        icon: Icons.waves_rounded,
        accentColor: marketAccent,
        isSelected: selectedIndex == 'Streamer',
        onTap: () => provider.selectIndex('Streamer'),
         subtitle: 'Live',
      ),
    ];

    final discoveryItems = [
      SecondarySidebarItem(
        title: 'Instrument Explorer',
        icon: Icons.manage_search_rounded,
        accentColor: marketAccent,
        isSelected: selectedIndex == 'Instrument Explorer',
        onTap: () => provider.selectIndex('Instrument Explorer'),
      ),
      SecondarySidebarItem(
        title: 'ETF Explorer',
        icon: Icons.dashboard_customize_rounded,
        accentColor: marketAccent,
        isSelected: selectedIndex == 'ETF Explorer',
        onTap: () => provider.selectIndex('ETF Explorer'),
      ),
      SecondarySidebarItem(
        title: 'Heatmap Explorer',
        icon: Icons.view_comfy_alt_rounded,
        accentColor: marketAccent,
        isSelected: selectedIndex == 'Heatmap Explorer',
        onTap: () => provider.selectIndex('Heatmap Explorer'),
      ),
       SecondarySidebarItem(
        title: 'Market Analysis',
        icon: Icons.analytics_rounded,
        accentColor: marketAccent,
        isSelected: selectedIndex == 'Market Analysis',
        onTap: () => provider.selectIndex('Market Analysis'),
      ),
      SecondarySidebarItem(
        title: 'Analysis Dashboard',
        icon: Icons.dashboard_customize_rounded, // Distinct icon
        accentColor: marketAccent,
        isSelected: selectedIndex == 'Analysis Dashboard',
        onTap: () => provider.selectIndex('Analysis Dashboard'),
      ),
      SecondarySidebarItem(
        title: 'Price Test',
        icon: Icons.price_check_rounded,
        accentColor: marketAccent,
        isSelected: selectedIndex == 'Price Test',
        onTap: () => provider.selectIndex('Price Test'),
      ),
    ];
    
     final systemToolsItems = [
       SecondarySidebarItem(
        title: 'Admin Dashboard',
        icon: Icons.admin_panel_settings_rounded,
        accentColor: const Color(0xFFFF6B6B), // Red for Admin
        isSelected: selectedIndex == 'Admin Dashboard',
        onTap: () => provider.selectIndex('Admin Dashboard'),
      ),
    ];


    // Build sections with dynamic headers
    final sections = <SecondarySidebarSection>[
      SecondarySidebarSection(
        title: 'INDICES',
        items: indicesItems,
        initiallyExpanded: true,
      ),
      SecondarySidebarSection(
        title: 'DISCOVERY', // Renamed from Tools/etc to match concept
        items: discoveryItems,
        initiallyExpanded: true,
      ),
       SecondarySidebarSection(
        title: 'SYSTEM',
        items: systemToolsItems,
        initiallyExpanded: true,
      ),
    ];

    // New Trade Button
    final newTradeButton = Container(
      width: double.infinity,
      height: 48,
      decoration: BoxDecoration(
        color: const Color(0xFF1F222B), // Dark background for contrast
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: Colors.white.withOpacity(0.1),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.2),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () {
            // Navigate to NEW TRADE (Action)
            // Implementation depends on app routing, placeholder for now
          },
          borderRadius: BorderRadius.circular(12),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: const [
              Icon(Icons.add, color: Colors.white, size: 20),
              SizedBox(width: 8),
              Text(
                'New Trade',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 14,
                ),
              ),
            ],
          ),
        ),
      ),
    );


    return SecondarySidebar(
      title: 'WORKSPACE',
      subtitle: 'Personal Account',
      icon: Icons.grid_view_rounded, // Workspace grid icon
      accentColor: marketAccent,
      width: 250,
      sections: sections,
      footer: newTradeButton,
    );
  }
}
