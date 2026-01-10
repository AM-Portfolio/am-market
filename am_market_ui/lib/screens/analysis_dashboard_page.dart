import 'package:flutter/material.dart';
import 'package:get_it/get_it.dart';
import 'package:am_market_ui/services/market_analysis_service.dart';
import 'package:am_market_ui/widgets/calendar_heatmap.dart';
import 'package:fl_chart/fl_chart.dart';

class AnalysisDashboardPage extends StatefulWidget {
  final String symbol;
  const AnalysisDashboardPage({Key? key, required this.symbol}) : super(key: key);

  @override
  State<AnalysisDashboardPage> createState() => _AnalysisDashboardPageState();
}

class _AnalysisDashboardPageState extends State<AnalysisDashboardPage> {
  final _analysisService = GetIt.I<MarketAnalysisService>();
  
  String _timeframe = 'DAY';
  int _heatmapYear = DateTime.now().year;
  
  Map<String, dynamic>? _technicalData;
  Map<String, dynamic>? _seasonalityData;
  Map<String, dynamic>? _heatmapData;
  
  bool _isLoading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final tech = await _analysisService.getTechnicalAnalysis(widget.symbol, timeframe: _timeframe);
      final seasonality = await _analysisService.getSeasonality(widget.symbol, timeframe: _timeframe);
      final heatmap = await _analysisService.getCalendarHeatmap(widget.symbol, year: _heatmapYear);

      if (mounted) {
        setState(() {
          _technicalData = tech;
          _seasonalityData = seasonality;
          _heatmapData = heatmap;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString();
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('${widget.symbol} Analysis'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      body: _isLoading 
          ? const Center(child: CircularProgressIndicator())
          : _error != null 
              ? Center(child: Text('Error: $_error')) 
              : _buildContent(),
    );
  }

  Widget _buildContent() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildTimeframeSelector(),
          const SizedBox(height: 16),
          if (_technicalData != null) _buildTechnicalCard(),
          const SizedBox(height: 24),
          if (_seasonalityData != null) _buildSeasonalitySection(),
          const SizedBox(height: 24),
          _buildHeatmapSection(),
        ],
      ),
    );
  }

  Widget _buildTimeframeSelector() {
    return Row(
      children: ['DAY', 'WEEK', 'MONTH'].map((tf) {
        final isSelected = _timeframe == tf;
        return Padding(
          padding: const EdgeInsets.only(right: 8.0),
          child: ChoiceChip(
            label: Text(tf),
            selected: isSelected,
            onSelected: (selected) {
              if (selected) {
                setState(() => _timeframe = tf);
                _loadData();
              }
            },
          ),
        );
      }).toList(),
    );
  }

  Widget _buildTechnicalCard() {
    final cur = _technicalData!['currentPrice']?.toStringAsFixed(2) ?? '-';
    final sma50 = _technicalData!['sma50']?.toStringAsFixed(2) ?? '-';
    final rsi = _technicalData!['rsi14']?.toStringAsFixed(2) ?? '-';
    final signal = _technicalData!['signal'] ?? 'NEUTRAL';

    Color signalColor = Colors.grey;
    if (signal == 'BUY') signalColor = Colors.green;
    if (signal == 'SELL') signalColor = Colors.red;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Technical Indicators', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _buildStat('Price', cur),
                _buildStat('SMA 50', sma50),
                _buildStat('RSI 14', rsi),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(color: signalColor.withOpacity(0.1), borderRadius: BorderRadius.circular(4)),
                  child: Text(signal, style: TextStyle(color: signalColor, fontWeight: FontWeight.bold)),
                )
              ],
            )
          ],
        ),
      ),
    );
  }

  Widget _buildStat(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
        Text(value, style: const TextStyle(fontWeight: FontWeight.bold)),
      ],
    );
  }

  Widget _buildSeasonalitySection() {
    final monthly = _seasonalityData!['monthlyReturns'] as Map<String, dynamic>?;
    
    if (monthly == null) return const SizedBox();

    // Prepare data for BarChart
    List<BarChartGroupData> barGroups = [];
    int i = 0;
    // Map month names to index 0-11
    final monthKeys = ['JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER'];
    
    for (var m in monthKeys) {
      double val = (monthly[m] ?? 0.0) as double;
      barGroups.add(BarChartGroupData(x: i, barRods: [
        BarChartRodData(toY: val, color: val >= 0 ? Colors.green : Colors.red, width: 16)
      ]));
      i++;
    }

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Monthly Seasonality (Avg Return %)', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 24),
            SizedBox(
              height: 200,
              child: BarChart(
                BarChartData(
                  barGroups: barGroups,
                  titlesData: FlTitlesData(
                    bottomTitles: AxisTitles(
                      sideTitles: SideTitles(showTitles: true, getTitlesWidget: (val, meta) {
                        if (val.toInt() >= 0 && val.toInt() < 12) {
                           return Text(monthKeys[val.toInt()].substring(0, 3), style: const TextStyle(fontSize: 10));
                        }
                        return const SizedBox();
                      })
                    ),
                    leftTitles: const AxisTitles(sideTitles: SideTitles(showTitles: true, reservedSize: 40)),
                    topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                    rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  ),
                  gridData: const FlGridData(show: true),
                  borderData: FlBorderData(show: false),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeatmapSection() {
    if (_heatmapData == null) return const SizedBox();
    
    // Parse nested map
    // { symbol: "...", year: 2025, data: { "JANUARY": { 1: 0.5, ... } } }
    final rawData = _heatmapData!['data'] as Map<String, dynamic>;
    final Map<String, Map<int, double>> parsedData = {};
    
    rawData.forEach((key, value) {
      if (value is Map) {
        parsedData[key] = (value).map((k, v) => MapEntry(int.parse(k.toString()), (v as num).toDouble()));
      }
    });

    return Card(
      child: Column(
        children: [
          // Year Selector Header
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text("Calendar Heatmap", style: Theme.of(context).textTheme.titleMedium),
                DropdownButton<int>(
                  value: _heatmapYear,
                  items: List.generate(10, (i) => DateTime.now().year - i).map((y) => DropdownMenuItem(value: y, child: Text(y.toString()))).toList(),
                  onChanged: (val) {
                    if (val != null) {
                      setState(() => _heatmapYear = val);
                      _loadData();
                    }
                  }
                ),
              ],
            ),
          ),
          SizedBox(
            height: 400,
            child: CalendarHeatmap(
              symbol: widget.symbol,
              year: _heatmapYear,
              data: parsedData,
            ),
          ),
        ],
      ),
    );
  }
}
