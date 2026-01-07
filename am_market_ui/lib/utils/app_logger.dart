import 'package:am_design_system/am_design_system.dart';

enum LogLevel {
  debug,
  info,
  warning,
  error,
}

/// Redirects to am_common_ui's CommonLogger for standardization
class CommonLogger {
  static void debug(String message, {String? tag}) {
    CommonLogger.debug(message, tag: tag);
  }

  static void info(String message, {String? tag}) {
    CommonLogger.info(message, tag: tag);
  }

  static void warning(String message, {String? tag, Object? error, StackTrace? stackTrace}) {
    CommonLogger.warning(message, tag: tag, error: error, stackTrace: stackTrace);
  }

  static void error(String message, {String? tag, Object? error, StackTrace? stackTrace}) {
    CommonLogger.error(message, tag: tag, error: error, stackTrace: stackTrace);
  }


  static void methodEntry(String methodName, {String? tag, Map<String, dynamic>? params}) {
    final paramsStr = params != null ? '($params)' : '()';
    CommonLogger.debug('→ $methodName$paramsStr', tag: tag);
  }

  static void methodExit(String methodName, {String? tag, dynamic result}) {
    final resultStr = result != null ? ' -> $result' : '';
    CommonLogger.debug('← $methodName$resultStr', tag: tag);
  }

  static void userAction(String action, {String? tag, Map<String, dynamic>? context}) {
    CommonLogger.info('User Action: $action context: $context', tag: tag ?? 'UserAction');
  }

  static void log({
    required LogLevel level,
    required String tag,
    required String message,
    Object? error,
    StackTrace? stackTrace,
  }) {
    switch (level) {
      case LogLevel.debug:
        CommonLogger.debug(message, tag: tag);
        break;
      case LogLevel.info:
        CommonLogger.info(message, tag: tag);
        break;
      case LogLevel.warning:
        CommonLogger.warning(message, tag: tag, error: error, stackTrace: stackTrace);
        break;
      case LogLevel.error:
        CommonLogger.error(message, tag: tag, error: error, stackTrace: stackTrace);
        break;
    }
  }
}



