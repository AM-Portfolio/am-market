import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:am_market_common/providers/market_provider.dart';
import 'package:am_market_common/models/ingestion_log.dart';
import 'package:am_market_dev/features/developer/services/admin_service.dart';

import 'package:am_design_system/am_design_system.dart';

class HistoricalSyncPage extends StatefulWidget {
  const HistoricalSyncPage({super.key});

  @override
  State<HistoricalSyncPage> createState() => _HistoricalSyncPageState();
}

class _HistoricalSyncPageState extends State<HistoricalSyncPage> with WidgetsBindingObserver {
  final AdminService _adminService = AdminService();
  
  // Controls State
  String _selectedProvider = 'UPSTOX';
  String _selectedSymbol = ''; // For custom symbol input
  // Pre-selected index dropdown value
  String? _selectedIndex; 

  final TextEditingController _symbolController = TextEditingController();
  bool _filtersExpanded = true;
  
  // New State
  DateTimeRange? _selectedDateRange;
  bool _forceRefresh = true;
  bool _fetchIndexStocks = false; // Whether to fetch individual stocks from index symbols

  // Data State
  List<IngestionLog> _logs = [];
  IngestionJobLog? _currentJob;
  bool _isLoading = false;
  Timer? _timer;
  bool _isStreaming = false; // Mock state for feed button visual
  bool _isPollingPaused = false; // Control polling state
  bool _isWidgetVisible = true; // Track if widget is currently visible

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    // Defer data fetching to avoid setState during build
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _fetchLogs();
        _startPolling();
      }
    });
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // Pause polling when app goes to background
    if (state == AppLifecycleState.paused || state == AppLifecycleState.inactive) {
      _pausePolling();
      CommonLogger.info("App inactive - paused polling", tag: "Admin");

    } else if (state == AppLifecycleState.resumed && _isWidgetVisible) {
      _resumePolling();
      CommonLogger.info("App resumed - resumed polling", tag: "Admin");

    }
  }

  @override
  void deactivate() {
    // Widget is being removed from the tree (navigating away)
    _isWidgetVisible = false;
    _isPollingPaused = true; // Update directly without setState
    CommonLogger.info("Widget deactivated - paused polling", tag: "Admin");
    super.deactivate();
  }

  @override
  void activate() {
    // Widget is being reinserted into the tree (navigating back)
    _isWidgetVisible = true;
    _isPollingPaused = false; // Update directly without setState
    _fetchLogs();
    CommonLogger.info("Widget activated - resumed polling", tag: "Admin");
    super.activate();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _stopPolling();
    _symbolController.dispose();
    super.dispose();
  }

  void _startPolling() {
    _stopPolling(); // Cancel any existing timer
    _timer = Timer.periodic(const Duration(seconds: 5), (timer) {
      if (mounted && !_isPollingPaused && _isWidgetVisible) {
        _fetchLogs();
      }
    });
    CommonLogger.info("Started polling for logs", tag: "Admin");

  }

  void _stopPolling() {
    _timer?.cancel();
    _timer = null;
    CommonLogger.info("Stopped polling for logs", tag: "Admin");

  }

  void _pausePolling() {
    setState(() => _isPollingPaused = true);
    CommonLogger.info("Paused polling for logs", tag: "Admin");

  }

  void _resumePolling() {
    setState(() => _isPollingPaused = false);
    CommonLogger.info("Resumed polling for logs", tag: "Admin");

  }

  Future<void> _fetchLogs() async {
    try {
      final logs = await _adminService.getLogs(
          startDate: _selectedDateRange?.start,
          endDate: _selectedDateRange?.end
      );
      if (mounted) {
        setState(() {
          _logs = logs;
          if (_logs.isNotEmpty && _currentJob == null) {
              // Initial load logic if needed, but usually we just show list
          }
           // Update current job stats if running? 
           // For now, let's just pick the latest one for the top cards
           if (_logs.isNotEmpty) {
               final latest = _logs.first;
               // We need IngestionJobLog type for the Cards, but _logs is IngestionLog
               // Map it strictly or usage loose types. properties match mostly.
               _currentJob = IngestionJobLog(
                   jobId: latest.jobId,
                   startTime: latest.startTime,
                   endTime: latest.endTime,
                   status: latest.status,
                   totalSymbols: latest.totalSymbols,
                   successCount: latest.successCount,
                   failureCount: latest.failureCount,
                   failedSymbols: latest.failedSymbols,
                   durationMs: latest.durationMs,
                   message: latest.message,
                   payloadSize: latest.payloadSize 
               );
           }
        });
      }
    } catch (e) {
      CommonLogger.error("Error fetching logs: $e", tag: "HistoricalSyncPage");

    }
  }

  Future<void> _triggerHistoricalSync() async {
    // Determine symbol: Text Input is the source of truth (populated by dropdown or manual)
    String symbol = _symbolController.text;
    
    if (symbol.isEmpty) {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Please select an Index or enter a Symbol")));
        return;
    }

    CommonLogger.info("Triggering sync for $symbol (Force: $_forceRefresh, Fetch Index Stocks: $_fetchIndexStocks)", tag: "Admin");

    // Removed crashing ScaffoldMessenger call here
    
    try {
        await _adminService.triggerHistoricalSync(
          symbol: symbol, 
          forceRefresh: _forceRefresh,
          fetchIndexStocks: _fetchIndexStocks,
        );
        if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Sync Triggered Successfully!")));
            _fetchLogs();
        }
    } catch (e) {
        if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Failed to trigger sync: $e"), backgroundColor: Colors.red));
        }
    }
  }

  Future<void> _stopIngestion() async {
      try {
          await _adminService.stopIngestion(_selectedProvider); // Uses selected provider
          _pausePolling(); // Stop polling when stopping ingestion
          if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Ingestion Stopped Successfully! (Polling paused)")));
      } catch (e) {
          if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Failed to stop ingestion: $e"), backgroundColor: Colors.red));
      }
  }

  void _startStream() {
      setState(() => _isStreaming = true);
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Feed Started (Simulated)")));
  }

  void _stopStream() {
      setState(() => _isStreaming = false);
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Feed Stopped")));
  }


  Future<void> _showJobDetails(String jobId) async {
    // Fetch full details including logs
    showDialog(
      context: context,
      builder: (ctx) => const Center(child: CircularProgressIndicator()),
    );
    
    try {
        final job = await _adminService.getJobDetails(jobId);
        Navigator.pop(context); // Close loading
        
        if (job != null) {
                                final logs = job.logs ?? [];
                                
                                showDialog(
                                    context: context,
                                    builder: (context) => Dialog(
                                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                                        child: Container(
                                            width: 800,
                                            height: 600,
                                            padding: const EdgeInsets.all(24),
                                            child: Column(
                                                crossAxisAlignment: CrossAxisAlignment.start,
                                                children: [
                                                    Row(
                                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                                        children: [
                                                            Text("Job Details: ${job.jobId}", style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                                                            IconButton(icon: const Icon(Icons.close), onPressed: () => Navigator.pop(context))
                                                        ],
                                                    ),
                                                    const Divider(),
                                                    const SizedBox(height: 10),
                                                    Wrap(
                                                        spacing: 20,
                                                        runSpacing: 10,
                                                        children: [
                                                            _detailItem("Status", job.status, color: job.status == 'SUCCESS' ? Colors.green : Colors.blue),
                                                            _detailItem("Duration", "${(job.durationMs/1000).toStringAsFixed(1)}s"),
                                                            _detailItem("Processed", "${job.totalSymbols}"),
                                                            _detailItem("Success", "${job.successCount}", color: Colors.green),
                                                            _detailItem("Failed", "${job.failureCount}", color: Colors.red),
                                                            _detailItem("Payload", _formatBytes(job.payloadSize)),
                                                        ],
                                                    ),
                                                    const SizedBox(height: 20),
                                                    const Text("Execution Logs", style: TextStyle(fontWeight: FontWeight.bold)),
                                                    const SizedBox(height: 10),
                                                    Expanded(
                                                        child: Container(
                                                            width: double.infinity,
                                                            padding: const EdgeInsets.all(12),
                                                            decoration: BoxDecoration(
                                                                color: Colors.black87,
                                                                borderRadius: BorderRadius.circular(8)
                                                            ),
                                                            child: ListView.builder(
                                                                itemCount: logs.length,
                                                                itemBuilder: (ctx, i) => Padding(
                                                                    padding: const EdgeInsets.only(bottom: 4),
                                                                    child: Text(
                                                                        logs[i], 
                                                                        style: const TextStyle(color: Colors.greenAccent, fontFamily: 'monospace', fontSize: 12)
                                                                    ),
                                                                ),
                                                            ),
                                                        ),
                                                    ),
                                if (job.failedSymbols.isNotEmpty) ...[
                                    const SizedBox(height: 20),
                                    const Text("Failed Symbols", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.red)),
                                    const SizedBox(height: 5),
                                    Container(
                                        padding: const EdgeInsets.all(8),
                                        decoration: BoxDecoration(color: Colors.red.withOpacity(0.1), borderRadius: BorderRadius.circular(4)),
                                        child: Text(job.failedSymbols.join(", "), style: const TextStyle(color: Colors.red)),
                                    )
                                ]
                            ],
                        ),
                    ),
                ),
            );
        }
    } catch (e) {
        Navigator.pop(context); // Close loading
        CommonLogger.error("Error fetching job details: $e", tag: "Admin");

    }
  }

  Widget _detailItem(String label, String value, {Color? color}) {
      return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
              Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
              Text(value, style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: color ?? Colors.black87)),
          ],
      );
  }

  @override
  Widget build(BuildContext context) {
      final theme = Theme.of(context);
      final isDark = theme.brightness == Brightness.dark;
      
      return Container(
          color: theme.scaffoldBackgroundColor, 
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Stats Cards
              if (_currentJob != null) ...[
                 Wrap(
                   spacing: 16,
                   runSpacing: 16,
                   children: [
                     _buildGlossyCard("Symbols Processed", "${_currentJob!.totalSymbols}", Icons.bar_chart, Colors.blue, theme),
                     _buildGlossyCard("Payload Size", _formatBytes(_currentJob!.payloadSize), Icons.data_usage, Colors.purple, theme),
                     _buildGlossyCard("Success", "${_currentJob!.successCount}", Icons.check_circle, Colors.green, theme),
                     _buildGlossyCard("Failed", "${_currentJob!.failureCount}", Icons.error, Colors.red, theme),
                   ],
                 ),
                 const SizedBox(height: 24),
              ],
              
              // Controls
              _buildControlsCard(theme),
              const SizedBox(height: 24),

              // Job History Table Header with Polling Status
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text("Job History", style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
                  Row(
                    children: [
                      // Polling status indicator
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: _isPollingPaused ? Colors.orange.withOpacity(0.1) : Colors.green.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(color: _isPollingPaused ? Colors.orange : Colors.green),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              _isPollingPaused ? Icons.pause_circle_filled : Icons.autorenew, 
                              size: 16, 
                              color: _isPollingPaused ? Colors.orange : Colors.green,
                            ),
                            const SizedBox(width: 6),
                            Text(
                              _isPollingPaused ? "Polling Paused" : "Auto-refresh ON",
                              style: TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                                color: _isPollingPaused ? Colors.orange : Colors.green,
                              ),
                            ),
                          ],
                        ),
                      ),
                      if (_isPollingPaused) ...[
                        const SizedBox(width: 8),
                        AppButton(
                           text: "Resume",
                           icon: Icons.play_arrow,
                           backgroundColor: Colors.green,
                           width: 100,
                           height: 32,
                           onPressed: _resumePolling,
                        ),
                      ],
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 10),
              
              Expanded(
                child: Container(
                  decoration: BoxDecoration(
                    color: theme.cardColor,
                    borderRadius: BorderRadius.circular(12),
                    boxShadow: [
                      BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4)),
                    ],
                  ),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(12),
                    child: SingleChildScrollView(
                      scrollDirection: Axis.vertical,
                      child: SingleChildScrollView(
                        scrollDirection: Axis.horizontal, 
                        child: ConstrainedBox(
                           constraints: BoxConstraints(minWidth: MediaQuery.of(context).size.width - 350), 
                           child: DataTable(
                              headingTextStyle: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                              dataTextStyle: theme.textTheme.bodyMedium,
                              columns: const [
                                DataColumn(label: Text('Job ID')),
                                DataColumn(label: Text('Start Time')),
                                DataColumn(label: Text('Status')),
                                DataColumn(label: Text('Duration')),
                                DataColumn(label: Text('Success')),
                                DataColumn(label: Text('Failed')), 
                                DataColumn(label: Text('Payload')), 
                                DataColumn(label: Text('Actions')),
                              ],
                              rows: _logs.map((log) => DataRow(cells: [
                                DataCell(Text(log.jobId.length > 8 ? log.jobId.substring(0, 8) : log.jobId)),
                                DataCell(Text(DateFormat('MMM dd, HH:mm:ss').format(log.startTime))),
                                DataCell(_buildStatusBadge(log.status)),
                                DataCell(Text("${(log.durationMs / 1000).toStringAsFixed(1)}s")),
                                DataCell(Text("${log.successCount}", style: const TextStyle(color: Colors.green, fontWeight: FontWeight.bold))),
                                DataCell(Text("${log.failureCount}", style: const TextStyle(color: Colors.red, fontWeight: FontWeight.bold))),
                                DataCell(Text(_formatBytes(log.payloadSize))),
                                DataCell(IconButton(
                                  icon: Icon(Icons.visibility, color: theme.iconTheme.color?.withOpacity(0.7)),
                                  onPressed: () => _showJobDetails(log.jobId),
                                )),
                              ])).toList(),
                            ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        );
  }

  // Helper to format bytes
  String _formatBytes(int bytes) {
    if (bytes < 1024) return "$bytes B";
    if (bytes < 1024 * 1024) return "${(bytes / 1024).toStringAsFixed(1)} KB";
    return "${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB";
  }

  Widget _buildGlossyCard(String title, String value, IconData icon, Color color, ThemeData theme) {
    final isDark = theme.brightness == Brightness.dark;
    return Container(
      width: 250,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: theme.cardColor,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: color.withOpacity(0.15),
            blurRadius: 15,
            offset: const Offset(0, 8),
          ),
        ],
        border: isDark ? Border.all(color: color.withOpacity(0.2)) : null,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(title, style: theme.textTheme.bodySmall?.copyWith(color: theme.hintColor, fontWeight: FontWeight.bold)),
              Icon(icon, color: color, size: 20),
            ],
          ),
          const SizedBox(height: 10),
          Text(value, style: theme.textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold, fontSize: 28)),
        ],
      ),
    );
  }

  Widget _buildControlsCard(ThemeData theme) {
    return Container(
      decoration: BoxDecoration(
        color: theme.cardColor,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        children: [
           // Header with Expand Toggle
           InkWell(
             onTap: () => setState(() => _filtersExpanded = !_filtersExpanded),
             child: Padding(
               padding: const EdgeInsets.all(16.0),
               child: Row(
                 mainAxisAlignment: MainAxisAlignment.spaceBetween,
                 children: [
                    Row(
                      children:  [
                        const Icon(Icons.tune, color: Colors.blueAccent),
                        const SizedBox(width: 10),
                        Text("Actions & Filters", style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                      ],
                    ),
                    Icon(_filtersExpanded ? Icons.expand_less : Icons.expand_more, color: theme.iconTheme.color),
                 ],
               ),
             ),
           ),
           
           if (_filtersExpanded)
             Padding(
               padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
               child: Column(
                 children: [
                   Divider(color: theme.dividerColor),
                   const SizedBox(height: 10),
                   Wrap(
                      spacing: 16,
                      runSpacing: 16,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        // Provider Dropdown
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12),
                          decoration: BoxDecoration(
                            border: Border.all(color: theme.dividerColor),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: DropdownButton<String>(
                            value: _selectedProvider,
                            underline: const SizedBox(),
                            icon: Icon(Icons.arrow_drop_down, color: theme.iconTheme.color),
                            style: theme.textTheme.bodyMedium,
                            dropdownColor: theme.cardColor,
                            items: ["UPSTOX", "ZERODHA"].map((e) => DropdownMenuItem(value: e, child: Text(e))).toList(),
                            onChanged: (val) => setState(() => _selectedProvider = val!),
                          ),
                        ),
                        
                        // Index Dropdown
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12),
                          decoration: BoxDecoration(
                            border: Border.all(color: theme.dividerColor),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: DropdownButton<String>( 
                           hint: Text("Select Index", style: theme.textTheme.bodyMedium?.copyWith(color: theme.hintColor)),
                           value: _selectedIndex,
                           underline: const SizedBox(),
                           icon: Icon(Icons.arrow_drop_down, color: theme.iconTheme.color),
                           style: theme.textTheme.bodyMedium,
                           dropdownColor: theme.cardColor,
                           items: ["NIFTY 50", "NIFTY BANK", "NIFTY 500"].map((e) => DropdownMenuItem(value: e, child: Text(e))).toList(),
                           onChanged: (val) {
                               setState(() {
                                   _selectedIndex = val;
                                   if (val != null) _symbolController.text = val;
                               });
                           }
                          ),
                        ),
                        
                        // Trigger Button
                        SizedBox(
                          width: 250,
                          child: TextField(
                            controller: _symbolController,
                            style: theme.textTheme.bodyMedium,
                            decoration: InputDecoration(
                              hintText: "Or Custom Symbol",
                              hintStyle: theme.textTheme.bodyMedium?.copyWith(color: theme.hintColor),
                              border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide(color: theme.dividerColor)),
                              enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide(color: theme.dividerColor)),
                              contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 0),
                              suffixIcon: IconButton(
                                icon: const Icon(Icons.clear, size: 16),
                                onPressed: () {
                                    _symbolController.clear();
                                    setState(() => _selectedIndex = null); // Clear dropdown too
                                },
                              ),
                            ),
                          ),
                        ),
                        
                        // Force Refresh
                        InkWell(
                          onTap: () => setState(() => _forceRefresh = !_forceRefresh),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Checkbox(
                                value: _forceRefresh,
                                onChanged: (val) => setState(() => _forceRefresh = val ?? true),
                              ),
                              Text("Force Refresh", style: theme.textTheme.bodyMedium),
                            ],
                          ),
                        ),

                        // Fetch Index Stocks
                        InkWell(
                          onTap: () => setState(() => _fetchIndexStocks = !_fetchIndexStocks),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Checkbox(
                                value: _fetchIndexStocks,
                                onChanged: (val) => setState(() => _fetchIndexStocks = val ?? false),
                              ),
                              Text("Fetch Stocks", style: theme.textTheme.bodyMedium),
                            ],
                          ),
                        ),
                        
                        // Trigger Button
                        AppButton(
                          onPressed: _triggerHistoricalSync,
                          icon: Icons.sync,
                          text: "Sync History",
                          backgroundColor: theme.primaryColor,
                          width: 180,
                        ),

                        // Stop Ingestion Button
                        AppButton(
                          onPressed: _stopIngestion,
                          icon: Icons.stop_circle_outlined,
                          text: "Stop Ingest",
                          backgroundColor: Colors.red,
                          textColor: Colors.red,
                          isOutlined: true,
                          width: 180,
                        ),
                        
                        // Stream Controls
                        if (!_isStreaming)
                          AppButton(
                             onPressed: _startStream,
                             icon: Icons.play_arrow,
                             text: "Start Feed",
                             backgroundColor: Colors.green,
                             textColor: Colors.green,
                             isOutlined: true,
                             width: 180,
                          )
                        else
                          AppButton(
                             onPressed: _stopStream,
                             icon: Icons.stop,
                             text: "Stop Feed",
                             backgroundColor: Colors.red,
                             textColor: Colors.red,
                             isOutlined: true,
                             width: 180,
                          )
                      ],
                   ),
                   const SizedBox(height: 16),
                   Wrap(
                       spacing: 20,
                       runSpacing: 16,
                       crossAxisAlignment: WrapCrossAlignment.center,
                       children: [
                           // Date Filter
                           OutlinedButton.icon(
                               onPressed: () async {
                                   final picked = await showDateRangePicker(
                                       context: context,
                                       firstDate: DateTime(2020),
                                       lastDate: DateTime.now(),
                                       builder: (context, child) {
                                         return Theme(
                                           data: theme.copyWith(
                                             colorScheme: theme.colorScheme.copyWith(
                                               surface: theme.cardColor,
                                             ),
                                           ),
                                           child: child!,
                                         );
                                       }
                                   );
                                   if (picked != null) {
                                       setState(() => _selectedDateRange = picked);
                                       _fetchLogs();
                                   }
                               },
                               icon: Icon(Icons.calendar_today, size: 18, color: theme.iconTheme.color),
                               label: Text(
                                   _selectedDateRange == null 
                                   ? "Filter by Date" 
                                   : "${DateFormat('MM/dd').format(_selectedDateRange!.start)} - ${DateFormat('MM/dd').format(_selectedDateRange!.end)}",
                                   style: theme.textTheme.bodyMedium,
                               ),
                               style: OutlinedButton.styleFrom(
                                 side: BorderSide(color: theme.dividerColor),
                                 padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                               ),
                           ),
                           if (_selectedDateRange != null)
                               IconButton(
                                   icon: const Icon(Icons.clear, size: 18), 
                                   onPressed: () {
                                       setState(() => _selectedDateRange = null);
                                       _fetchLogs();
                                   }
                               ),
                       ],
                   ),
                 ],
               ),
             ),
        ],
      ),
    );
  }

  Widget _buildStatusBadge(String status) {
    Color color;
    switch (status) {
      case 'SUCCESS': color = Colors.green; break;
      case 'FAILED': color = Colors.red; break;
      case 'PARTIAL_SUCCESS': color = Colors.orange; break;
      default: color = Colors.blue;
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withOpacity(0.5)),
      ),
      child: Text(status, style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.bold)),
    );
  }
}

// Temporary data class if model isn't sufficient for view logic
class IngestionJobLog {
    final String jobId;
    final DateTime startTime;
    final DateTime? endTime;
    final String status;
    final int totalSymbols;
    final int successCount;
    final int failureCount;
    final List<String> failedSymbols;
    final double durationMs;
    final String? message;
    final int payloadSize;
    final List<String> logs;

    IngestionJobLog({
        required this.jobId,
        required this.startTime,
        this.endTime,
        required this.status,
        required this.totalSymbols,
        required this.successCount,
        required this.failureCount,
        required this.failedSymbols,
        required this.durationMs,
        this.message,
        this.payloadSize = 0,
        this.logs = const [],
    });
}
