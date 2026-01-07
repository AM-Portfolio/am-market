import 'package:am_design_system/am_design_system.dart';

import 'dart:async';
import 'package:flutter/material.dart';
import '../services/api_service.dart';
import '../services/stream_service.dart';
import 'package:intl/intl.dart';
import 'package:url_launcher/url_launcher.dart';

import 'package:provider/provider.dart';
import '../providers/market_provider.dart';


class StreamerPage extends StatefulWidget {
  const StreamerPage({super.key});

  @override
  State<StreamerPage> createState() => _StreamerPageState();
}

class _StreamerPageState extends State<StreamerPage> {
  final ApiService _apiService = ApiService();
  final StreamService _streamService = StreamService();
  
  // Config State
  String _provider = 'UPSTOX'; // UPSTOX, ZERODHA
  String _exchangeSegment = 'None'; 
  bool _autoPrefix = true;
  bool _isIndexSymbol = false;
  final TextEditingController _symbolsController = TextEditingController(text: 'NIFTY 50'); 
  final TextEditingController _searchController = TextEditingController();
  
  // Live Data State
  List<Map<String, dynamic>> _feedHistory = []; 
  Map<String, dynamic> _quotes = {}; 
  List<String> _logs = [];
  bool _isStreaming = false;

  // Pagination State
  int _currentPage = 0;
  final int _itemsPerPage = 100;

  // Search State
  List<Map<String, dynamic>> _searchResults = [];
  bool _isSearching = false;
  
  StreamSubscription? _subscription;
  MarketProvider? _marketProvider; 

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    try {
      _marketProvider = Provider.of<MarketProvider>(context, listen: false);
    } catch (e) {
      _log("didChangeDependencies Error: $e", method: "StreamerPage.didChangeDependencies", level: LogLevel.error);
    }
  }

  @override
  void initState() {
    super.initState();
    _streamService.connect();
    
    // Listen to stream
    _subscription = _streamService.stream.listen((message) {
       if (!mounted) return; 
       
       if (message.containsKey('quotes')) {
         try {
           final provider = _marketProvider;
           final bool hasProvider = provider != null;

           setState(() {
             final newQuotes = message['quotes'] as Map<String, dynamic>;
             final now = DateTime.now(); 
           
             newQuotes.forEach((key, val) {
               val['timestamp'] = now;
               val['symbol'] = key; 
               
               _feedHistory.insert(0, val);
               _quotes[key] = val; 
               
               if (mounted && hasProvider) {
                  try {
                    provider!.updateLivePrice(val);
                  } catch (e) {
                    // Suppress
                  }
               }
             });
              
             if (_feedHistory.length > 500) {
               _feedHistory = _feedHistory.sublist(0, 500);
             }
           });
         } catch (e) {
           _log("Error processing update: $e", method: "StreamerPage.streamListener", level: LogLevel.error);
         }
       }
    });
  }

  @override
  void dispose() {
    _subscription?.cancel();
    _streamService.dispose();
    _symbolsController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  void _log(String msg, {String method = 'StreamerPage', LogLevel level = LogLevel.info}) {
    if (mounted) {
      setState(() {
        _logs.insert(0, "[${DateFormat('HH:mm:ss').format(DateTime.now())}] $msg");
        if (_logs.length > 50) _logs.removeLast();
      });
    }
    switch (level) {
      case LogLevel.error:
        CommonLogger.error(msg, tag: method);
        break;
      case LogLevel.warning:
        CommonLogger.warning(msg, tag: method);
        break;
      case LogLevel.debug:
        CommonLogger.debug(msg, tag: method);
        break;
      case LogLevel.info:
      default:
        CommonLogger.info(msg, tag: method);
        break;
    }
  }

  // --- Actions ---

  Future<void> _getLoginUrl() async {
    final url = await _apiService.getLoginUrl(_provider);
    if (url != null) {
      _log("Login URL generated: $url");
      if (!mounted) return;
      
      // Open URL in new tab
      try {
        final uri = Uri.parse(url);
        // Use url_launcher to open in new tab
        if (await canLaunchUrl(uri)) {
          await launchUrl(uri, mode: LaunchMode.externalApplication);
          _log("Opened login URL in new tab");
        } else {
          _log("Could not launch URL", method: "StreamerPage._getLoginUrl", level: LogLevel.error);
          // Fallback to showing dialog
          showDialog(
            context: context,
            builder: (ctx) => AlertDialog(
              title: const Text("Login URL"),
              content: SelectableText(url),
              actions: [TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Close"))],
            ),
          );
        }
      } catch (e) {
        _log("Error opening URL: $e", method: "StreamerPage._getLoginUrl", level: LogLevel.error);
        // Fallback to showing dialog
        showDialog(
          context: context,
          builder: (ctx) => AlertDialog(
            title: const Text("Login URL"),
            content: SelectableText(url),
            actions: [TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("Close"))],
          ),
        );
      }
    } else {
      _log("Failed to get Login URL");
    }
  }

  Future<void> _startStream() async {
    final raw = _symbolsController.text;
    if (raw.isEmpty) {
      _log("Please enter symbols", method: "StreamerPage._startStream", level: LogLevel.warning);
      return;
    }
    
    List<String> symbols = raw.split(',').map((e) => e.trim().toUpperCase()).where((e) => e.isNotEmpty).toList();
    if (symbols.isEmpty) return;

    if (_autoPrefix && _exchangeSegment != 'None') {
      symbols = symbols.map((s) {
        if (s.contains('|')) return s; 
        return "$_exchangeSegment|$s";
      }).toList();
    }

    final success = await _apiService.connectStream(symbols, _provider, isIndexSymbol: _isIndexSymbol);
    if (success) {
      setState(() => _isStreaming = true);
      _log("Stream Connect Request Sent: OK", method: "StreamerPage._startStream");
    } else {
      _log("Stream Connect Failed", method: "StreamerPage._startStream", level: LogLevel.error);
    }
  }

  Future<void> _stopStream() async {
    final success = await _apiService.disconnectStream(_provider);
    if (success) {
       setState(() => _isStreaming = false);
       _log("Stream Stop Request Sent", method: "StreamerPage._stopStream");
    } else {
      _log("Stop Stream Failed", method: "StreamerPage._stopStream", level: LogLevel.error);
    }
  }

  Future<void> _search() async {
    final query = _searchController.text;
    if (query.isEmpty) return;

    setState(() => _isSearching = true);
    final results = await _apiService.searchInstruments(query, _provider);
    setState(() {
      _searchResults = results;
      _isSearching = false;
    });
    _log("Found ${results.length} instruments for '$query'", method: "StreamerPage._search");
  }

  void _addSymbol(String symbol) {
    final current = _symbolsController.text;
    if (current.isNotEmpty && !current.endsWith(',')) {
      _symbolsController.text = "$current, $symbol";
    } else {
      _symbolsController.text = "$current$symbol";
    }
    _log("Added $symbol");
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    
    return Scaffold(
        backgroundColor: theme.scaffoldBackgroundColor,
        appBar: AppBar(
            title: Text("Market Data Streamer", style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold)), 
            backgroundColor: theme.appBarTheme.backgroundColor,
            elevation: 0,
            iconTheme: theme.iconTheme,
        ),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildConfigPanel(context),
              const SizedBox(width: 24),
              Expanded(child: _buildRightPanel(context)),
            ],
          ),
        ),
      );
  }

  // --- UI Helper Methods ---

  Widget _buildConfigPanel(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      width: 350,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: theme.cardColor,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
           BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4)),
        ]
      ),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text("Configuration", style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 20),
            
            // Auth Provider
            Text("Auth Provider", style: theme.textTheme.bodySmall?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 5),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              decoration: BoxDecoration(
                border: Border.all(color: theme.dividerColor), 
                borderRadius: BorderRadius.circular(8),
                color: theme.inputDecorationTheme.fillColor ?? theme.canvasColor,
              ),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<String>(
                  value: _provider,
                  isExpanded: true,
                  style: theme.textTheme.bodyMedium,
                  dropdownColor: theme.cardColor,
                  items: ['UPSTOX', 'ZERODHA'].map((e) => DropdownMenuItem(value: e, child: Text(e))).toList(),
                  onChanged: (val) => setState(() => _provider = val!),
                ),
              ),
            ),
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton(
                onPressed: _getLoginUrl,
                style: OutlinedButton.styleFrom(
                    foregroundColor: theme.primaryColor, 
                    side: BorderSide(color: theme.primaryColor),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8))
                ),
                child: const Text("Login & Get Token"),
              ),
            ),
            
            const SizedBox(height: 20),
            
            // Exchange Segment
            Text("Exchange Segment", style: theme.textTheme.bodySmall?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 5),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              decoration: BoxDecoration(
                border: Border.all(color: theme.dividerColor), 
                borderRadius: BorderRadius.circular(8),
                color: theme.inputDecorationTheme.fillColor ?? theme.canvasColor,
              ),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<String>(
                  value: _exchangeSegment,
                  isExpanded: true,
                  style: theme.textTheme.bodyMedium,
                  dropdownColor: theme.cardColor,
                  items: [
                    'NSE_EQ', 'NFO', 'CDS', 'MCX', 'BSE_EQ', 'BSE_FO', 'None'
                  ].map((e) => DropdownMenuItem(value: e, child: Text(e == 'NSE_EQ' ? 'NSE Equity (NSE_EQ)' : e))).toList(),
                  onChanged: (val) => setState(() => _exchangeSegment = val!),
                ),
              ),
            ),
            
            const SizedBox(height: 10),
            Row(
              children: [
                Checkbox(
                  value: _autoPrefix, 
                  onChanged: (val) => setState(() => _autoPrefix = val!),
                  activeColor: theme.primaryColor,
                ),
                Text("Auto-prefix Exchange?", style: theme.textTheme.bodyMedium),
              ],
            ),
            
            Row(
              children: [
                Checkbox(
                  value: _isIndexSymbol, 
                  onChanged: (val) => setState(() => _isIndexSymbol = val!),
                  activeColor: theme.primaryColor,
                ),
                Text("Is Index Symbol?", style: theme.textTheme.bodyMedium),
              ],
            ),
            
            const SizedBox(height: 10),
            Text("Symbols (Comma Separated)", style: theme.textTheme.bodySmall?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 5),
            TextField(
              controller: _symbolsController,
              decoration: InputDecoration(
                hintText: "e.g. INFY, RELIANCE, TCS",
                hintStyle: theme.inputDecorationTheme.hintStyle,
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide(color: theme.dividerColor)),
                enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide(color: theme.dividerColor)),
                filled: true,
                fillColor: theme.inputDecorationTheme.fillColor ?? theme.canvasColor,
              ),
              maxLines: 3,
              style: theme.textTheme.bodyMedium,
            ),
            Text("Enter symbols without exchange if Auto-prefix is on.", style: theme.textTheme.bodySmall?.copyWith(fontSize: 10)),
            
            const SizedBox(height: 20),
            
            Row(
              children: [
                Expanded(
                  child: ElevatedButton(
                    onPressed: _startStream,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: theme.primaryColor,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                    child: const Text("Start Stream"),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: ElevatedButton(
                    onPressed: _stopStream,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: theme.colorScheme.error,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                    child: const Text("Stop"),
                  ),
                ),
              ],
            ),
            
            const SizedBox(height: 24),
            Text("System Logs", style: theme.textTheme.bodySmall?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 5),
            Container(
              height: 150,
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.8),
                  borderRadius: BorderRadius.circular(8)
              ),
              child: ListView.builder(
                reverse: true,
                itemCount: _logs.length,
                itemBuilder: (ctx, i) => Text(_logs[i], style: const TextStyle(color: Colors.greenAccent, fontSize: 10, fontFamily: 'monospace')),
              ),
            )
          ],
        ),
      ),
    );
  }

  Widget _buildRightPanel(BuildContext context) {
    return Container(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSearchSection(context),
          if (_isSearching) const Padding(padding: EdgeInsets.only(top: 10), child: LinearProgressIndicator()),
          const SizedBox(height: 20),
          Expanded(
            child: Column(
              children: [
                if (_searchResults.isNotEmpty) Expanded(child: _buildSearchResults(context)),
                if (_searchResults.isEmpty && _feedHistory.isEmpty && _quotes.isEmpty)
                   Padding(
                     padding: const EdgeInsets.all(32.0),
                     child: Center(
                       child: Text("Enter a query to find instruments or start a stream.", style: Theme.of(context).textTheme.titleMedium?.copyWith(color: Theme.of(context).disabledColor)),
                     ),
                   ),
                if (_feedHistory.isNotEmpty || _quotes.isNotEmpty) 
                  Expanded(flex: 2, child: _buildLiveFeedSection(context)),
              ],
            ),
          )
        ],
      ),
    );
  }

  Widget _buildSearchSection(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
            color: theme.cardColor,
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4)),
            ]
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                 Icon(Icons.search, size: 28, color: theme.primaryColor),
                 const SizedBox(width: 8),
                 Text("Instrument Search", style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
              ],
            ),
            const SizedBox(height: 20),
            Row(
                children: [
                    Expanded(
                        child: TextField(
                          controller: _searchController,
                          decoration: InputDecoration(
                            hintText: "Search Symbol (e.g. Reliance, Nifty Bank)...",
                            hintStyle: theme.inputDecorationTheme.hintStyle,
                            border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide(color: theme.dividerColor)),
                            enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: BorderSide(color: theme.dividerColor)),
                            filled: true,
                            fillColor: theme.inputDecorationTheme.fillColor ?? theme.canvasColor,
                          ),
                          style: theme.textTheme.bodyMedium,
                        ),
                    ),
                    const SizedBox(width: 10),
                    ElevatedButton(
                        onPressed: _search,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: theme.primaryColor,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 32),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                        ),
                        child: const Text("Search", style: TextStyle(fontWeight: FontWeight.bold)),
                      ),
                ],
            )
          ],
        ),
    );
  }

  Widget _buildSearchResults(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      decoration: BoxDecoration(
        color: theme.cardColor,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4)),
        ],
      ),
      child: ListView.separated(
        padding: const EdgeInsets.all(8),
        separatorBuilder: (ctx, i) => Divider(color: theme.dividerColor),
        itemCount: _searchResults.length,
        itemBuilder: (context, index) {
          final item = _searchResults[index];
          final symbol = item['tradingSymbol'] ?? item['symbol'] ?? 'Unknown';
          final key = item['instrumentKey'] ?? symbol;
          final name = item['name'] ?? '';
          final exchange = item['exchange'] ?? '';
          
          return ListTile(
            title: Text(symbol, style: theme.textTheme.bodyLarge?.copyWith(fontWeight: FontWeight.bold)),
            subtitle: Text("$name ($exchange)", style: theme.textTheme.bodySmall),
            trailing: IconButton(
              icon: Icon(Icons.add_circle_outline, color: theme.primaryColor), 
              onPressed: () => _addSymbol(key)
            ),
            onTap: () => _addSymbol(key),
          );
        },
      ),
    );
  }

  Widget _buildLiveFeedSection(BuildContext context) {
    final theme = Theme.of(context);
    final startIndex = _currentPage * _itemsPerPage;
    final endIndex = (startIndex + _itemsPerPage < _feedHistory.length) 
        ? startIndex + _itemsPerPage 
        : _feedHistory.length;
    
    final currentItems = _feedHistory.sublist(startIndex, endIndex);
    final totalPages = (_feedHistory.length / _itemsPerPage).ceil();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (_searchResults.isNotEmpty) const SizedBox(height: 20),
        Row(
           mainAxisAlignment: MainAxisAlignment.spaceBetween,
           children: [
              Row(
                children: [
                    Icon(Icons.monitor_heart, color: theme.primaryColor),
                    const SizedBox(width: 8),
                    Text("Live Feed", style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold)),
                ],
              ),
              if (_feedHistory.isNotEmpty)
                Row(
                  children: [
                    IconButton(
                      icon: Icon(Icons.first_page, color: theme.iconTheme.color?.withOpacity(0.6)),
                      onPressed: _currentPage > 0 ? () => setState(() => _currentPage = 0) : null,
                    ),
                    IconButton(
                      icon: Icon(Icons.chevron_left, color: theme.iconTheme.color?.withOpacity(0.6)),
                      onPressed: _currentPage > 0 ? () => setState(() => _currentPage--) : null,
                    ),
                    Text(
                      "Page ${_currentPage + 1} / ${totalPages == 0 ? 1 : totalPages} (${_feedHistory.length} items)", 
                      style: theme.textTheme.bodySmall
                    ),
                    IconButton(
                      icon: Icon(Icons.chevron_right, color: theme.iconTheme.color?.withOpacity(0.6)),
                      onPressed: _currentPage < totalPages - 1 ? () => setState(() => _currentPage++) : null,
                    ),
                    IconButton(
                      icon: Icon(Icons.last_page, color: theme.iconTheme.color?.withOpacity(0.6)),
                      onPressed: _currentPage < totalPages - 1 ? () => setState(() => _currentPage = totalPages - 1) : null,
                    ),
                  ],
                ),
           ],
        ),
        const SizedBox(height: 10),
        Expanded(
          child: Container(
             width: double.infinity,
             decoration: BoxDecoration(
               color: theme.cardColor,
               borderRadius: BorderRadius.circular(16),
               boxShadow: [
                 BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4)),
               ],
             ),
             child: ClipRRect(
               borderRadius: BorderRadius.circular(16),
               child: SingleChildScrollView(
                  scrollDirection: Axis.vertical,
                  child: Theme(
                    data: theme.copyWith(dividerColor: theme.dividerColor),
                    child: SizedBox(
                      width: double.infinity,
                      child: DataTable(
                        headingRowColor: MaterialStateProperty.all(theme.canvasColor),
                        dataRowColor: MaterialStateProperty.all(theme.cardColor),
                        columnSpacing: 20,
                        headingTextStyle: theme.textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold),
                        dataTextStyle: theme.textTheme.bodyMedium,
                        columns: const [
                          DataColumn(label: Text('Time')),
                          DataColumn(label: Text('Symbol')),
                          DataColumn(label: Text('LTP')),
                          DataColumn(label: Text('Change')),
                          DataColumn(label: Text('% Change')),
                          DataColumn(label: Text('Prev Close')),
                        ],
                        rows: currentItems.map((data) {
                          final key = data['symbol'] ?? 'UNKNOWN';
                          final ltp = (data['lastPrice'] as num?)?.toDouble() ?? 0.0;
                          final change = (data['change'] as num?)?.toDouble() ?? 0.0;
                          final pChange = (data['changePercent'] as num?)?.toDouble() ?? 0.0;
                          final color = change >= 0 ? Colors.green : Colors.red;
                          final time = data['timestamp'] as DateTime? ?? DateTime.now();
                          final prevClose = ltp - change;
                          
                          // Improve Time Visibility
                          final timeStr = DateFormat('HH:mm:ss').format(time);

                          return DataRow(
                            cells: [
                              DataCell(Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                decoration: BoxDecoration(
                                  color: theme.primaryColor.withOpacity(0.1), 
                                  borderRadius: BorderRadius.circular(4),
                                  border: Border.all(color: theme.primaryColor.withOpacity(0.1)),
                                ),
                                child: Text(timeStr, style: TextStyle(color: theme.primaryColor, fontWeight: FontWeight.bold, fontFamily: 'monospace')),
                              )),
                              DataCell(Text(key, style: const TextStyle(fontWeight: FontWeight.bold))),
                              DataCell(Text(ltp.toStringAsFixed(2), style: const TextStyle(fontWeight: FontWeight.w600))),
                              DataCell(Text(change.toStringAsFixed(2), style: TextStyle(color: color, fontWeight: FontWeight.bold))),
                              DataCell(Text('${pChange.toStringAsFixed(2)}%', style: TextStyle(color: color, fontWeight: FontWeight.bold))),
                              DataCell(Text(prevClose.toStringAsFixed(2), style: TextStyle(color: theme.disabledColor))),
                            ],
                          );
                        }).toList(),
                      ),
                    ),
                  ),
               ),
             ),
          ),
        ),
      ],
    );
  }
}
