import 'package:flutter/material.dart';

class InstrumentExplorerPage extends StatelessWidget {
  const InstrumentExplorerPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Instrument Explorer')),
      body: const Center(child: Text('Instrument Explorer (Restored)')),
    );
  }
}
