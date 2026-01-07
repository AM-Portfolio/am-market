import 'package:flutter/material.dart';
import '../../services/market_analysis_service.dart';

class AnalysisPage extends StatefulWidget {
  final String? initialSymbol;

  const AnalysisPage({Key? key, this.initialSymbol}) : super(key: key);

  @override
  _AnalysisPageState createState() => _AnalysisPageState();
}

class _AnalysisPageState extends State<AnalysisPage> {
  final _service = MarketAnalysisService();
  final _symbolController = TextEditingController();
  
  bool _isLoading = false;
  Map<String, dynamic>? _results;
  String? _error;

  @override
  void initState() {
    super.initState();
    if (widget.initialSymbol != null) {
      _symbolController.text = widget.initialSymbol!;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _analyze();
      });
    }
  }

  Future<void> _analyze() async {
    final symbol = _symbolController.text.trim().toUpperCase();
    if (symbol.isEmpty) return;

    setState(() {
      _isLoading = true;
      _error = null;
      _results = null;
    });

    try {
      final data = await _service.analyzeSymbol(symbol);
      setState(() {
        _results = data;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Market Analysis'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _symbolController,
                    decoration: const InputDecoration(
                      labelText: 'Symbol (e.g., RELIANCE)',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                ElevatedButton(
                  onPressed: _isLoading ? null : _analyze,
                  child: const Text('Analyze'),
                ),
              ],
            ),
            const SizedBox(height: 20),
            if (_isLoading) const CircularProgressIndicator(),
            if (_error != null) 
              Text('Error: $_error', style: const TextStyle(color: Colors.red)),
            if (_results != null) ...[
              Text(
                'Results for ${_results!['symbol']}',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 10),
              Expanded(
                child: ListView(
                  children: _buildResultItems(_results!['results']),
                ),
              ),
            ]
          ],
        ),
      ),
    );
  }

  List<Widget> _buildResultItems(Map<String, dynamic>? results) {
    if (results == null) return [];
    
    return results.entries.map((entry) {
      // The value is likely a List of values. 
      // We'll show the last (most recent) value for simplicity, 
      // or the whole list if short.
      final val = entry.value;
      String displayVal = val.toString();
      
      if (val is List && val.isNotEmpty) {
        // Show last non-null value
        final last = val.lastWhere((e) => e != null, orElse: () => null);
        displayVal = last?.toString() ?? "No Data";
      }

      return Card(
        child: ListTile(
          title: Text(entry.key.toUpperCase()), // e.g. SMA_50
          trailing: Text(displayVal, style: const TextStyle(fontWeight: FontWeight.bold)),
        ),
      );
    }).toList();
  }
}
