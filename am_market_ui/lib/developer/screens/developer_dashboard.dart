import 'package:flutter/material.dart';
import '../widgets/market_data_tester_widget.dart';
import '../widgets/scheduler_control_widget.dart';

class DeveloperDashboard extends StatelessWidget {
  const DeveloperDashboard({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Developer Dashboard'),
        backgroundColor: Colors.deepPurple,
      ),
      body: SingleChildScrollView(
        child: Column(
          children: const [
            SchedulerControlWidget(),
            MarketDataTesterWidget(),
          ],
        ),
      ),
    );
  }
}
