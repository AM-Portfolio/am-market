/// Market Module - Embeddable Market Data Widget
/// 
/// This module provides a complete market data interface that can be
/// embedded into any Flutter application.
/// 
/// ## Usage
/// 
/// ```dart
/// import 'package:am_market_ui/market_module.dart';
/// import 'package:am_design_system/am_design_system.dart';
/// 
/// // In your widget tree:
/// ModuleContainer(
///   module: MarketModule(),
///   moduleContext: ModuleContext(
///     userId: currentUser.id,
///     userName: currentUser.name,
///     userEmail: currentUser.email,
///    isAuthenticated: true,
///   ),
/// )
/// ```

library market_module;

// Export the main module
export 'modules/market_module.dart';

// Export widgets for advanced customization
export 'modules/widgets/market_sidebar_content.dart';
export 'modules/widgets/market_main_content.dart';

// Export provider for state management
export 'providers/market_provider.dart';
