import 'package:flutter/material.dart';
import '../services/developer_scheduler_service.dart';

class SchedulerControlWidget extends StatefulWidget {
  const SchedulerControlWidget({super.key});

  @override
  State<SchedulerControlWidget> createState() => _SchedulerControlWidgetState();
}

class _SchedulerControlWidgetState extends State<SchedulerControlWidget> {
  final DeveloperSchedulerService _service = DeveloperSchedulerService();
  List<String> _schedulers = [];
  bool _isLoading = false;
  String? _statusMessage;

  @override
  void initState() {
    super.initState();
    _loadSchedulers();
  }

  Future<void> _loadSchedulers() async {
    setState(() => _isLoading = true);
    try {
      final jobs = await _service.getSchedulers();
      setState(() {
        _schedulers = jobs;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _statusMessage = 'Error: $e';
        _isLoading = false;
      });
    }
  }

  Future<void> _triggerScheduler(String jobName) async {
    setState(() => _statusMessage = 'Triggering $jobName...');
    try {
      final response = await _service.triggerScheduler(jobName);
      setState(() {
        _statusMessage = 'Success: ${response['message']}';
      });
    } catch (e) {
      setState(() {
        _statusMessage = 'Failed to trigger $jobName';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.all(16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Backend Schedulers',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                IconButton(
                  icon: const Icon(Icons.refresh),
                  onPressed: _loadSchedulers,
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (_statusMessage != null)
              Container(
                padding: const EdgeInsets.all(8),
                color: Colors.blue.withOpacity(0.1),
                child: Text(_statusMessage!),
              ),
            const SizedBox(height: 16),
            if (_isLoading)
              const Center(child: CircularProgressIndicator())
            else if (_schedulers.isEmpty)
              const Text('No schedulers found.')
            else
              ListView.separated(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: _schedulers.length,
                separatorBuilder: (context, index) => const Divider(),
                itemBuilder: (context, index) {
                  final job = _schedulers[index];
                  return ListTile(
                    title: Text(job),
                    trailing: ElevatedButton(
                      onPressed: () => _triggerScheduler(job),
                      child: const Text('Run Now'),
                    ),
                  );
                },
              ),
          ],
        ),
      ),
    );
  }
}
