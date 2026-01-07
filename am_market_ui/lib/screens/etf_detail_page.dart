import 'package:flutter/material.dart';
import '../models/etf.dart';
import '../services/etf_service.dart';

class EtfDetailPage extends StatefulWidget {
  final String symbol;
  final String name;
  final VoidCallback? onBack;

  const EtfDetailPage({super.key, required this.symbol, required this.name, this.onBack});

  @override
  State<EtfDetailPage> createState() => _EtfDetailPageState();
}

class _EtfDetailPageState extends State<EtfDetailPage> {
  final EtfService _etfService = EtfService();
  EtfHoldings? _holdings;
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadHoldings();
  }

  Future<void> _loadHoldings() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final holdings = await _etfService.getEtfHoldings(widget.symbol);
      if (holdings == null) {
        // Try triggering a fetch if not found
        final triggered = await _etfService.triggerFetchHoldings(widget.symbol);
        if (triggered) {
          // Wait a bit and try again (polling approach simplfied)
          await Future.delayed(const Duration(seconds: 2));
           final retry = await _etfService.getEtfHoldings(widget.symbol);
           if (mounted) {
             setState(() {
               _holdings = retry;
               _isLoading = false;
               if (retry == null) _error = "Fetching data... please check back later.";
             });
           }
        } else {
             if (mounted) setState(() {
               _isLoading = false;
               _error = "Details not available for this ETF.";
             });
        }
      } else {
         if (mounted) setState(() {
           _holdings = holdings;
           _isLoading = false;
         });
      }
    } catch (e) {
      if (mounted) setState(() {
        _isLoading = false;
        _error = "Error loading details: $e";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final content = _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.error_outline, size: 48, color: Colors.orange),
                      const SizedBox(height: 10),
                      Text(_error!, style: const TextStyle(fontSize: 16)),
                      const SizedBox(height: 10),
                      ElevatedButton(
                        onPressed: _loadHoldings,
                        child: const Text("Retry"),
                      )
                    ],
                  ),
                )
              : _buildContent();

    // If onBack is provided, render inline (no Scaffold)
    if (widget.onBack != null) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Inline Header
          Row(
            children: [
              IconButton(onPressed: widget.onBack, icon: const Icon(Icons.arrow_back)),
              const SizedBox(width: 8),
              Expanded(child: Text(widget.name, style: Theme.of(context).textTheme.headlineSmall)),
            ],
          ),
          const Divider(),
          Expanded(child: content),
        ],
      );
    }
    
    // Default Scaffold behavior (standalone)
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.name),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 1,
      ),
      body: content,
    );
  }

  Widget _buildContent() {
    if (_holdings == null || _holdings!.holdings.isEmpty) {
      return const Center(child: Text("No holdings data available."));
    }

    // Calculate totals
    double totalPercent = _holdings!.holdings.fold(0, (sum, item) => sum + item.percentage);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildInfoCard(),
          const SizedBox(height: 20),
          Text("Holdings Breakdown (${_holdings!.holdings.length}) - Total: ${totalPercent.toStringAsFixed(2)}%", 
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 10),
          Card(
            elevation: 2,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            child: DataTable(
              columns: const [
                DataColumn(label: Text('Stock Name', style: TextStyle(fontWeight: FontWeight.bold))),
                DataColumn(label: Text('ISIN', style: TextStyle(fontWeight: FontWeight.bold))),
                DataColumn(label: Text('Percentage', style: TextStyle(fontWeight: FontWeight.bold))),
                DataColumn(label: Text('Market Value', style: TextStyle(fontWeight: FontWeight.bold))),
              ],
              rows: _holdings!.holdings.map((h) {
                return DataRow(cells: [
                  DataCell(Text(h.stockName)),
                  DataCell(Text(h.isinCode)),
                  DataCell(Text("${h.percentage.toStringAsFixed(2)}%")),
                  DataCell(Text(h.marketValue != null ? h.marketValue!.toStringAsFixed(2) : '-')),
                ]);
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoCard() {
    return Card(
      color: Colors.blue.shade50,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          children: [
             Column(
               crossAxisAlignment: CrossAxisAlignment.start,
               children: [
                 Text("Symbol", style: TextStyle(color: Colors.grey.shade600, fontSize: 12)),
                 Text(widget.symbol, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
               ],
             ),
             const SizedBox(width: 40),
             Column(
               crossAxisAlignment: CrossAxisAlignment.start,
               children: [
                 Text("Holdings Count", style: TextStyle(color: Colors.grey.shade600, fontSize: 12)),
                 Text("${_holdings?.holdingsCount ?? 0}", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
               ],
             ),
          ],
        ),
      ),
    );
  }
}
