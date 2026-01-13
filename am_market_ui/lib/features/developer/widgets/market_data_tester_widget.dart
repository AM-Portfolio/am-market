import 'package:flutter/material.dart';
import 'dart:convert';
import '../services/developer_market_data_service.dart';

class MarketDataTesterWidget extends StatefulWidget {
  const MarketDataTesterWidget({super.key});

  @override
  State<MarketDataTesterWidget> createState() => _MarketDataTesterWidgetState();
}

class _MarketDataTesterWidgetState extends State<MarketDataTesterWidget> {
  final DeveloperMarketDataService _service = DeveloperMarketDataService();
  final TextEditingController _symbolController = TextEditingController(text: 'NSE:RELIANCE');
  bool _forceRefresh = false;
  String _result = '';
  bool _isLoading = false;

  Future<void> _fetch(Future<Map<String, dynamic>> Function() apiCall) async {
    setState(() {
      _isLoading = true;
      _result = 'Fetching...';
    });
    try {
      final data = await apiCall();
      setState(() {
        const JsonEncoder encoder = JsonEncoder.withIndent('  ');
        _result = encoder.convert(data);
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _result = 'Error: $e';
        _isLoading = false;
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
            const Text(
              'Market Data Tester',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _symbolController,
              decoration: const InputDecoration(
                labelText: 'Symbol(s)',
                border: OutlineInputBorder(),
                hintText: 'NSE:RELIANCE, NSE:INFY',
              ),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Checkbox(
                  value: _forceRefresh,
                  onChanged: (v) => setState(() => _forceRefresh = v ?? false),
                ),
                const Text('Force Refresh (Bypass Cache)'),
              ],
            ),
            const SizedBox(height: 16),
            Wrap(
              spacing: 8,
              children: [
                ElevatedButton(
                  onPressed: _isLoading
                      ? null
                      : () => _fetch(() => _service.getQuotes(_symbolController.text, refresh: _forceRefresh)),
                  child: const Text('Quotes'),
                ),
                ElevatedButton(
                  onPressed: _isLoading
                      ? null
                      : () => _fetch(() => _service.getOHLC(_symbolController.text, refresh: _forceRefresh)),
                  child: const Text('OHLC'),
                ),
                ElevatedButton(
                  onPressed: _isLoading
                      ? null
                      : () => _fetch(() => _service.getHistorical(_symbolController.text, refresh: _forceRefresh)),
                  child: const Text('Historical (7D)'),
                ),
              ],
            ),
            const SizedBox(height: 16),
            const Text('Result:', style: TextStyle(fontWeight: FontWeight.bold)),
            Container(
              height: 200,
              width: double.infinity,
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: Colors.grey[200],
                border: Border.all(color: Colors.grey),
              ),
              child: SingleChildScrollView(
                child: Text(
                  _result,
                  style: const TextStyle(fontFamily: 'Courier', fontSize: 12, color: Colors.black87),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
