import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:am_design_system/am_design_system.dart';
import 'package:am_market_common/services/api_service.dart';

class AdminDashboardPage extends StatefulWidget {
  const AdminDashboardPage({super.key});

  @override
  State<AdminDashboardPage> createState() => _AdminDashboardPageState();
}

class _AdminDashboardPageState extends State<AdminDashboardPage> {
  bool _isLoading = false;
  String? _lastResult;

  Future<void> _triggerJob(String name, String endpoint) async {
    setState(() {
      _isLoading = true;
      _lastResult = null;
    });

    try {
      final api = context.read<ApiService>();
      await api.triggerScheduler(endpoint);
      if (mounted) {
        setState(() {
          _lastResult = "Success: $name triggered";
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Triggered $name successfully')),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _lastResult = "Error: $e";
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to trigger $name')),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Admin Dashboard'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (_isLoading)
              const LinearProgressIndicator(),
            if (_lastResult != null)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 8.0),
                child: Text(
                  _lastResult!,
                  style: TextStyle(
                    color: _lastResult!.startsWith("Error") ? Colors.red : Colors.green,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            const SizedBox(height: 16),
            const Text(
              "Historical Data Sync",
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            _buildHistoricalSyncSection(),
            const SizedBox(height: 24),
            const Text(
              "Manual Scheduler Triggers",
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            Wrap(
              spacing: 8.0,
              runSpacing: 8.0,
              children: [
                _buildTriggerButton("Indices Process", "indices/process"),
                _buildTriggerButton("Indices Retry", "indices/retry"),
                _buildTriggerButton("Cookie Refresh", "cookie/refresh"),
                _buildTriggerButton("Start Streamer", "streamer/start"),
                _buildTriggerButton("Stop Streamer", "streamer/stop"),
                _buildTriggerButton("Morning Indices", "indices/morning"),
                _buildTriggerButton("Evening Indices", "indices/evening"),
                _buildTriggerButton("Redis Cleanup", "redis/cleanup"),
                _buildTriggerButton("Market Open", "market/open"),
                _buildTriggerButton("Market Close", "market/close"),
              ],
            ),
          ],
        ),
      ),
    );
  }

  // Inputs for Historical Sync
  final TextEditingController _symbolController = TextEditingController();
  String _selectedDuration = '1Y';
  bool _forceRefresh = true;

  Widget _buildHistoricalSyncSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _symbolController,
                    decoration: const InputDecoration(
                      labelText: 'Symbol / Index (e.g. NIFTY 50)',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                DropdownButton<String>(
                  value: _selectedDuration,
                  items: ['1D', '1W', '1M', '3M', '6M', '1Y', '3Y', '5Y', '10Y']
                      .map((e) => DropdownMenuItem(value: e, child: Text(e)))
                      .toList(),
                  onChanged: (v) => setState(() => _selectedDuration = v!),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                 Switch(
                   value: _forceRefresh, 
                   onChanged: (v) => setState(() => _forceRefresh = v)
                 ),
                 const Text("Force Refresh from Provider"),
                 const Spacer(),
                 ElevatedButton.icon(
                   onPressed: _isLoading ? null : _triggerHistoricalSync,
                   icon: const Icon(Icons.sync),
                   label: const Text("Run Sync"),
                 ),
              ],
            )
          ],
        ),
      ),
    );
  }

  Future<void> _triggerHistoricalSync() async {
    if (_symbolController.text.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Please enter a symbol')));
        return;
    }
    
    setState(() {
      _isLoading = true;
      _lastResult = null;
    });

    try {
      final api = context.read<ApiService>();
      await api.triggerHistoricalSync(
        symbol: _symbolController.text,
        duration: _selectedDuration,
        forceRefresh: _forceRefresh,
      );
      if (mounted) {
        setState(() {
          _lastResult = "Success: Historical sync triggered for ${_symbolController.text}";
        });
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Triggered successfully')));
      }
    } catch (e) {
       if (mounted) {
        setState(() => _lastResult = "Error: $e");
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Widget _buildTriggerButton(String label, String endpoint) {
    return ElevatedButton(
      onPressed: _isLoading ? null : () => _triggerJob(label, endpoint),
      child: Text(label),
    );
  }
}
