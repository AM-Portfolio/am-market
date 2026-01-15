import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:am_common/core/di/price_providers.dart';

class MarketStreamTestWidget extends ConsumerWidget {
  const MarketStreamTestWidget({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final pricesAsync = ref.watch(priceStreamProvider);
    final isConnectedAsync = ref.watch(priceConnectionStatusProvider);

    return Card(
      margin: const EdgeInsets.all(16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ListTile(
            title: const Text('Market Data Stream (Unified)'),
            subtitle: isConnectedAsync.when(
              data: (connected) => Text(
                connected ? 'Connected' : 'Disconnected',
                style: TextStyle(color: connected ? Colors.green : Colors.red),
              ),
              loading: () => const Text('Connecting...'),
              error: (e, s) => Text('Error: $e'),
            ),
            trailing: IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: () {
                // Trigger manual reconnect if needed, or just let service handle it
                ref.read(priceServiceProvider).value?.connect();
              },
            ),
          ),
          const Divider(),
          SizedBox(
            height: 200,
            child: pricesAsync.when(
              data: (prices) {
                if (prices.isEmpty) {
                  return const Center(child: Text('No price data received yet'));
                }
                return ListView.builder(
                  itemCount: prices.length,
                  itemBuilder: (context, index) {
                    final symbol = prices.keys.elementAt(index);
                    final quote = prices[symbol];
                    final price = quote?.lastPrice;
                    return ListTile(
                      dense: true,
                      title: Text(symbol),
                      subtitle: Text('Vol: ${quote?.open}'), // Just an example usage
                      trailing: Text(
                        price?.toStringAsFixed(2) ?? '--',
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
                    );
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, s) => Center(child: Text('Error: $e')),
            ),
          ),
        ],
      ),
    );
  }
}
