import 'package:flutter/material.dart';

class SecurityExplorerPage extends StatelessWidget {
  const SecurityExplorerPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Security Explorer')),
      body: const Center(child: Text('Security Explorer (Restored)')),
    );
  }
}
