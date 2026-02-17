library am_market_ui;

/// Main app entry point and core features
/// Export widgets and pages available for external use

// Export core pages
export 'features/dashboard/presentation/pages/dashboard_page.dart' show MarketPage;

// Export providers for Trade UI integration
// Export providers for Trade UI integration
export 'features/market_analysis/providers/market_analysis_providers.dart';

// Export shared widgets
export 'shared/widgets/trading_view_chart_widget.dart';

// Export domain models required by widgets
export 'features/market_analysis/internal/domain/models/chart_config.dart';
