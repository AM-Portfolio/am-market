import 'package:intl/intl.dart';

/// Utility class for formatting numbers, dates, and timestamps
class PriceFormatters {
  /// Format a number to 2 decimal places
  /// Returns '-' if value is null
  static String formatNumber(dynamic value) {
    if (value == null) return '-';
    if (value is num) return value.toStringAsFixed(2);
    return value.toString();
  }

  /// Format a date to 'dd MMM yyyy' format
  /// Example: 25 Dec 2025
  static String formatDate(DateTime date) {
    return DateFormat('dd MMM yyyy').format(date);
  }

  /// Format a date to short format 'dd MMM'
  /// Example: 25 Dec
  static String formatDateShort(DateTime date) {
    return DateFormat('dd MMM').format(date);
  }

  /// Format a timestamp to 'dd/MM HH:mm' format
  /// Handles both int (milliseconds) and String formats
  /// Returns '-' if parsing fails
  static String formatTimestamp(dynamic timestamp) {
    if (timestamp == null) return '-';
    
    try {
      if (timestamp is int) {
        final date = DateTime.fromMillisecondsSinceEpoch(timestamp);
        return DateFormat('dd/MM HH:mm').format(date);
      }
      if (timestamp is String) {
        final date = DateTime.parse(timestamp);
        return DateFormat('dd/MM HH:mm').format(date);
      }
    } catch (e) {
      // Return original value if parsing fails
    }
    
    return timestamp.toString();
  }

  /// Parse a timestamp to DateTime
  /// Handles both int (milliseconds) and String formats
  /// Returns null if parsing fails
  static DateTime? parseTimestamp(dynamic timestamp) {
    if (timestamp == null) return null;
    
    try {
      if (timestamp is int) {
        return DateTime.fromMillisecondsSinceEpoch(timestamp);
      }
      if (timestamp is String) {
        return DateTime.parse(timestamp);
      }
    } catch (e) {
      return null;
    }
    
    return null;
  }

  /// Format date range as "from - to"
  /// Example: "23 Dec 2025 - 25 Dec 2025"
  static String formatDateRange(DateTime from, DateTime to) {
    return '${formatDate(from)} - ${formatDate(to)}';
  }
}
