class IngestionLog {
  final String id;
  final String jobId;
  final DateTime startTime;
  final DateTime? endTime;
  final String status;
  final int totalSymbols;
  final int successCount;
  final int failureCount;
  final List<String> failedSymbols;
  final double durationMs;
  final int payloadSize;
  final String? message;
  final List<String>? logs;

  IngestionLog({
    required this.id,
    required this.jobId,
    required this.startTime,
    this.endTime,
    required this.status,
    required this.totalSymbols,
    required this.successCount,
    required this.failureCount,
    required this.failedSymbols,
    required this.durationMs,
    this.payloadSize = 0,
    this.message,
    this.logs,
  });

  factory IngestionLog.fromJson(Map<String, dynamic> json) {
    return IngestionLog(
      id: json['id'] as String,
      jobId: json['jobId'] as String,
      startTime: DateTime.parse(json['startTime']),
      endTime: json['endTime'] != null ? DateTime.parse(json['endTime']) : null,
      status: json['status'] as String,
      totalSymbols: json['totalSymbols'] as int,
      successCount: json['successCount'] as int,
      failureCount: json['failureCount'] as int,
      failedSymbols: (json['failedSymbols'] as List<dynamic>?)?.map((e) => e as String).toList() ?? [],
      durationMs: (json['durationMs'] as num).toDouble(),
      payloadSize: (json['payloadSize'] as num?)?.toInt() ?? 0,
      message: json['message'] as String?,
      logs: (json['logs'] as List<dynamic>?)?.map((e) => e as String).toList(),
    );
  }
}
