/// Application mode configuration
enum AppMode {
  /// Developer mode - Direct SDK access for testing and integration
  developer,
  
  /// User mode - SDK wrapped with abstraction layer and mapped models
  user,
}

/// Extension to provide mode-specific configurations
extension AppModeExtension on AppMode {
  /// Display name for the mode
  String get displayName {
    switch (this) {
      case AppMode.developer:
        return 'Developer';
      case AppMode.user:
        return 'User';
    }
  }

  /// Icon for the mode
  String get icon {
    switch (this) {
      case AppMode.developer:
        return '🔧';
      case AppMode.user:
        return '👤';
    }
  }

  /// Whether developer features should be visible
  bool get isDeveloperMode => this == AppMode.developer;

  /// Whether user features should be visible  
  bool get isUserMode => this == AppMode.user;
}
