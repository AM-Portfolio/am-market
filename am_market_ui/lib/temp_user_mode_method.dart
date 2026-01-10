  // User Mode - Simplified Navigation (Dashboard, Overview, Heatmap)
  List<SecondarySidebarSection> _buildUserModeSections(MarketProvider provider) {
    const accentColor = ModuleColors.market;
    final currentIndex = _swipeController.currentIndex;

    final userItems = [
      _createSidebarItem(0, 'Dashboard', Icons.home_rounded, 'Overview'),
      _createSidebarItem(1, 'Market Analysis', Icons.analytics_rounded, 'Detailed charts'),
      _createSidebarItem(2, 'Heatmap', Icons.grid_on_rounded, 'Calendar view'),
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
