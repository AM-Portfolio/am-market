import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:am_design_system/am_design_system.dart';
import 'package:am_market_ui/services/api_service.dart';

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

  Widget _buildTriggerButton(String label, String endpoint) {
    return ElevatedButton(
      onPressed: _isLoading ? null : () => _triggerJob(label, endpoint),
      child: Text(label),
    );
  }
}
