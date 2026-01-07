import 'package:flutter/material.dart';
import '../core/app_mode.dart';

/// Provider to manage application mode (Developer vs User)
class ModeProvider with ChangeNotifier {
  AppMode _currentMode = AppMode.user; // Default to user mode

  /// Get current application mode
  AppMode get currentMode => _currentMode;

  /// Check if in developer mode
  bool get isDeveloperMode => _currentMode.isDeveloperMode;

  /// Check if in user mode
  bool get isUserMode => _currentMode.isUserMode;

  /// Switch to specified mode
  void setMode(AppMode mode) {
    if (_currentMode != mode) {
      _currentMode = mode;
      notifyListeners();
    }
  }

  /// Toggle between developer and user mode
  void toggleMode() {
    _currentMode = _currentMode == AppMode.developer 
        ? AppMode.user 
        : AppMode.developer;
    notifyListeners();
  }
}
