import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:intl/intl.dart';
import 'package:am_market_ui/shared/widgets/stock_chart.dart';
import 'package:am_market_ui/shared/widgets/time_range_selector.dart';
import 'package:am_market_common/services/api_service.dart';
import 'package:am_market_ui/features/etf/services/etf_service.dart';

import 'package:provider/provider.dart';
import 'package:am_market_common/providers/market_provider.dart';

class StockDetailPage extends StatefulWidget {
  final String symbol;

  const StockDetailPage({Key? key, required this.symbol}) : super(key: key);

  @override
  _StockDetailPageState createState() => _StockDetailPageState();
}

class _StockDetailPageState extends State<StockDetailPage> {
  final ApiService _apiService = ApiService();
  List<Map<String, dynamic>> _chartData = [];
  bool _isLoading = true;
  String? _error;
  String _selectedRange = '1D'; // '1D' or '5Y'

  @override
  void initState() {
    super.initState();
    _fetchData();
  }

  Future<void> _fetchData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final data = await _apiService.fetchHistory(widget.symbol, _selectedRange);
      // Sort data by time ascending (oldest to newest)
      data.sort((a, b) {
        final dateA = DateTime.tryParse(a['time'].toString()) ?? DateTime.now();
        final dateB = DateTime.tryParse(b['time'].toString()) ?? DateTime.now();
        return dateA.compareTo(dateB);
      });

      setState(() {
        _chartData = data;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    // Listen to MarketProvider for live updates using Stream to avoid full rebuilds
    final marketProvider = Provider.of<MarketProvider>(context, listen: false);

    return Scaffold(
      backgroundColor: const Color(0xFF1E1E2E), // Dark Theme Background
      appBar: AppBar(
        title: StreamBuilder<Map<String, dynamic>>(
          stream: marketProvider.livePriceStream.where((event) => event['symbol'] == widget.symbol),
          builder: (context, snapshot) {
            final liveData = marketProvider.livePrices[widget.symbol];
            
            double? ltp;
            double? change;
            double? pChange;
            Color color = Colors.grey;

            if (liveData != null) {
               ltp = (liveData['lastPrice'] as num).toDouble();
               change = (liveData['change'] as num).toDouble();
               pChange = (liveData['changePercent'] as num).toDouble();
               color = change >= 0 ? Colors.greenAccent : Colors.redAccent;
            }

            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(widget.symbol),
                if (ltp != null)
                  Text(
                    "₹${ltp.toStringAsFixed(2)}  ${change! >= 0 ? '+' : ''}${change.toStringAsFixed(2)} (${pChange!.toStringAsFixed(2)}%)",
                     style: TextStyle(fontSize: 12, color: color),
                  ),
              ],
            );
          }
        ),
        backgroundColor: const Color(0xFF2E2E3E),
      ),
      body: Column(
        children: [
          // Range Selector
          TimeRangeSelector(
            selectedRange: _selectedRange,
            onRangeSelected: (range) {
              if (_selectedRange != range) {
                setState(() {
                  _selectedRange = range;
                });
                _fetchData();
              }
            },
            ranges: const ['10m', '15m', '30m', '1H', '4H', '1D', '1W', '1M', '5Y'],
          ),
          
          // Chart Area
          Expanded(
            child: StockChart(
              chartData: _chartData,
              isLoading: _isLoading,
              error: _error,
            ),
          ),
        ],
      ),
    );
  }
}
