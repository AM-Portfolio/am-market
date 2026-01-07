import 'package:flutter/material.dart';
import '../services/api_service.dart';

class InstrumentExplorerPage extends StatefulWidget {
  const InstrumentExplorerPage({super.key});

  @override
  State<InstrumentExplorerPage> createState() => _InstrumentExplorerPageState();
}

class _InstrumentExplorerPageState extends State<InstrumentExplorerPage> {
  final ApiService _apiService = ApiService();

  // Filter State
  final TextEditingController _queryController = TextEditingController();
  final TextEditingController _isinController = TextEditingController();
  
  final List<String> _selectedExchanges = [];
  final List<String> _selectedSegments = [];
  final List<String> _selectedTypes = [];
  
  bool _isLoading = false;
  List<Map<String, dynamic>> _results = [];
  String? _error;

  // Options
  final List<String> _exchanges = ['NSE', 'NFO', 'BSE', 'MCX'];
  final List<String> _segments = ['NSE_EQ', 'NSE_FO', 'BSE_EQ', 'MCX_FO'];
  final List<String> _types = ['EQUITY', 'FUTURE', 'OPTION', 'INDEX'];

  bool _hasSearched = false;

  @override
  void initState() {
    super.initState();
    // Default Defaults
    _selectedExchanges.add('NSE');
    _selectedTypes.add('INDEX');
  }

  @override
  void dispose() {
    _queryController.dispose();
    _isinController.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    setState(() {
      _isLoading = true;
      _error = null;
      _hasSearched = true;
    });

    try {
      final criteria = {
        'queries': _queryController.text.isNotEmpty ? [_queryController.text] : [],
        'isins': _isinController.text.isNotEmpty ? [_isinController.text] : [],
        'exchanges': _selectedExchanges.isNotEmpty ? _selectedExchanges : null,
        'segments': _selectedSegments.isNotEmpty ? _selectedSegments : null,
        'instrumentTypes': _selectedTypes.isNotEmpty ? _selectedTypes : null,
        'provider': 'UPSTOX' // Default to Upstox for now
      };

      final results = await _apiService.advancedSearchInstruments(criteria);
      
      setState(() {
        _results = results;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  void _clearFilters() {
    _queryController.clear();
    _isinController.clear();
    setState(() {
      _selectedExchanges.clear();
      _selectedSegments.clear();
      _selectedTypes.clear();
      _results.clear();
      _hasSearched = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    
    // White Theme Overrides for local scope if needed, or rely on Theme.of(context)
    return Container(
        color: theme.scaffoldBackgroundColor, // Use theme background
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Instrument Explorer',
                  style: theme.textTheme.headlineMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: theme.textTheme.bodyLarge?.color 
                  ),
                ),
                IconButton(
                  icon: Icon(Icons.refresh, color: theme.primaryColor),
                  onPressed: _clearFilters,
                  tooltip: 'Clear Filters',
                )
              ],
            ),
            const SizedBox(height: 16),
            
            // Filters Card
            Container(
              decoration: BoxDecoration(
                  color: theme.cardColor,
                  borderRadius: BorderRadius.circular(16),
                  boxShadow: [
                      BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4)),
                  ]
              ),
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _queryController,
                            style: theme.textTheme.bodyMedium,
                            decoration: InputDecoration(
                              labelText: 'Search (Name/Symbol)',
                              labelStyle: TextStyle(color: theme.hintColor),
                              border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                              prefixIcon: Icon(Icons.search, color: theme.iconTheme.color?.withOpacity(0.6)),
                              filled: true,
                              fillColor: theme.cardColor,
                            ),
                            onSubmitted: (_) => _search(),
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: TextField(
                            controller: _isinController,
                            style: theme.textTheme.bodyMedium,
                            decoration: InputDecoration(
                              labelText: 'ISIN',
                              labelStyle: TextStyle(color: theme.hintColor),
                              border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                              prefixIcon: Icon(Icons.qr_code, color: theme.iconTheme.color?.withOpacity(0.6)),
                              filled: true,
                              fillColor: theme.cardColor,
                            ),
                            onSubmitted: (_) => _search(),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    
                    // Toggles
                    _buildMultiSelect(context, 'Exchanges', _exchanges, _selectedExchanges),
                    const SizedBox(height: 8),
                    _buildMultiSelect(context, 'Segments', _segments, _selectedSegments),
                    const SizedBox(height: 8),
                    _buildMultiSelect(context, 'Types', _types, _selectedTypes),
                    
                    const SizedBox(height: 24),
                    ElevatedButton.icon(
                      onPressed: _search, 
                      icon: const Icon(Icons.search),
                      label: const Text('Search Instruments'),
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(horizontal: 40, vertical: 20),
                        backgroundColor: theme.primaryColor,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                      ),
                    )
                  ],
                ),
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Results
            Expanded(
              child: _isLoading 
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                  ? Center(child: Text('Error: $_error', style: TextStyle(color: theme.colorScheme.error)))
                  : !_hasSearched
                    ? Center(child: Text('Enter criteria and click search to view instruments', style: TextStyle(color: theme.hintColor)))
                    : _results.isEmpty
                      ? Center(child: Text('No instruments found. Adjust filters and search.', style: TextStyle(color: theme.disabledColor)))
                      : _buildResultsTable(context),
            ),
          ],
        ),
      );
  }

  Widget _buildMultiSelect(BuildContext context, String label, List<String> options, List<String> selected) {
    final theme = Theme.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 80, 
          child: Padding(
            padding: const EdgeInsets.only(top: 10),
            child: Text(label, style: TextStyle(color: theme.textTheme.bodyMedium?.color?.withOpacity(0.7), fontWeight: FontWeight.bold)),
          )
        ),
        Expanded(
          child: Wrap(
            spacing: 8,
            runSpacing: 4,
            children: options.map((opt) {
              final isSelected = selected.contains(opt);
              return FilterChip(
                label: Text(opt),
                selected: isSelected,
                onSelected: (val) {
                  setState(() {
                    if (val) {
                      selected.add(opt);
                    } else {
                      selected.remove(opt);
                    }
                  });
                },
                backgroundColor: theme.canvasColor,
                selectedColor: theme.primaryColor.withOpacity(0.1),
                labelStyle: TextStyle(color: isSelected ? theme.primaryColor : theme.textTheme.bodyMedium?.color, fontWeight: isSelected ? FontWeight.bold : FontWeight.normal),
                checkmarkColor: theme.primaryColor,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20), side: BorderSide(color: isSelected ? theme.primaryColor : theme.dividerColor)),
              );
            }).toList(),
          ),
        )
      ],
    );
  }

  Widget _buildResultsTable(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
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
          child: PaginatedDataTable(
            headingRowColor: MaterialStateProperty.all(theme.canvasColor),
            columns: [
              DataColumn(label: Text('Trading Symbol', style: TextStyle(color: theme.textTheme.bodyLarge?.color, fontWeight: FontWeight.bold))),
              DataColumn(label: Text('Name', style: TextStyle(color: theme.textTheme.bodyLarge?.color, fontWeight: FontWeight.bold))),
              DataColumn(label: Text('Exchange', style: TextStyle(color: theme.textTheme.bodyLarge?.color, fontWeight: FontWeight.bold))),
              DataColumn(label: Text('Segment', style: TextStyle(color: theme.textTheme.bodyLarge?.color, fontWeight: FontWeight.bold))),
              DataColumn(label: Text('Type', style: TextStyle(color: theme.textTheme.bodyLarge?.color, fontWeight: FontWeight.bold))),
              DataColumn(label: Text('ISIN', style: TextStyle(color: theme.textTheme.bodyLarge?.color, fontWeight: FontWeight.bold))),
              DataColumn(label: Text('Instrument Key', style: TextStyle(color: theme.textTheme.bodyLarge?.color, fontWeight: FontWeight.bold))),
            ],
            source: _InstrumentDataSource(_results, theme),
            rowsPerPage: _results.isEmpty ? 1 : (_results.length < 100 ? _results.length : 100),
            availableRowsPerPage: const [10, 20, 50, 100, 200],
            onRowsPerPageChanged: (val) {
               // Basic support
            },
            showCheckboxColumn: false,
            arrowHeadColor: theme.iconTheme.color,
          ),
        ),
      ),
    );
  }
}

class _InstrumentDataSource extends DataTableSource {
  final List<Map<String, dynamic>> _data;
  final ThemeData theme;

  _InstrumentDataSource(this._data, this.theme);

  @override
  DataRow? getRow(int index) {
    if (index >= _data.length) return null;
    final item = _data[index];
    
    return DataRow(
      cells: [
        DataCell(Text(item['trading_symbol'] ?? '-', style: TextStyle(color: theme.textTheme.bodyMedium?.color, fontWeight: FontWeight.bold))),
        DataCell(SizedBox(width: 200, child: Text(item['name'] ?? '-', style: TextStyle(color: theme.textTheme.bodyMedium?.color?.withOpacity(0.7)), overflow: TextOverflow.ellipsis))),
        DataCell(Text(item['exchange'] ?? '-', style: TextStyle(color: theme.textTheme.bodyMedium?.color))),
        DataCell(Text(item['segment'] ?? '-', style: TextStyle(color: theme.textTheme.bodyMedium?.color))),
        DataCell(Text(item['instrument_type'] ?? '-', style: TextStyle(color: theme.textTheme.bodyMedium?.color))),
        DataCell(Text(item['isin'] ?? '-', style: TextStyle(color: theme.textTheme.bodyMedium?.color))),
        DataCell(Text(item['instrument_key'] ?? '-', style: TextStyle(color: theme.hintColor, fontSize: 11))),
      ],
    );
  }

  @override
  bool get isRowCountApproximate => false;

  @override
  int get rowCount => _data.length;

  @override
  int get selectedRowCount => 0;
}
