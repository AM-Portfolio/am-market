import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/api_service.dart';

class SecurityExplorerPage extends StatefulWidget {
  const SecurityExplorerPage({super.key});

  @override
  State<SecurityExplorerPage> createState() => _SecurityExplorerPageState();
}

class _SecurityExplorerPageState extends State<SecurityExplorerPage> {
  final TextEditingController _queryController = TextEditingController();
  final TextEditingController _indexController = TextEditingController();
  
  List<dynamic> _securities = [];
  bool _isLoading = false;
  String _errorMessage = '';

  @override
  void dispose() {
    _queryController.dispose();
    _indexController.dispose();
    super.dispose();
  }

  Future<void> _searchSecurities() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final apiService = Provider.of<ApiService>(context, listen: false);
      
      final Map<String, dynamic> request = {};
      if (_queryController.text.isNotEmpty) {
        request['query'] = _queryController.text;
      }
      if (_indexController.text.isNotEmpty) {
        request['index'] = _indexController.text;
      }
      
      final results = await apiService.searchSecuritiesAdvanced(request);
      setState(() {
        _securities = results;
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Error searching securities: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Color _getCapColor(String? type) {
    if (type == null) return Colors.grey;
    switch (type.toUpperCase()) {
      case 'LARGE_CAP': return Colors.green;
      case 'MID_CAP': return Colors.orange;
      case 'SMALL_CAP': return Colors.blueAccent;
      default: return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    
    return Scaffold(
        appBar: AppBar(
          title: const Text('Security Explorer', style: TextStyle(fontWeight: FontWeight.bold)),
          elevation: 0,
        ),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Filter Section
              Container(
                 padding: const EdgeInsets.all(24),
                 decoration: BoxDecoration(
                   color: theme.cardColor,
                   borderRadius: BorderRadius.circular(16),
                   boxShadow: [
                     BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4)),
                   ]
                 ),
                 child: Column(
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: TextField(
                              controller: _queryController,
                              style: theme.textTheme.bodyMedium,
                              decoration: InputDecoration(
                                labelText: 'Search (Symbol, ISIN)',
                                labelStyle: TextStyle(color: theme.hintColor),
                                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                                prefixIcon: Icon(Icons.search, color: theme.iconTheme.color?.withOpacity(0.6)),
                                filled: true,
                                fillColor: theme.cardColor, // Or a slightly different shade if defined in theme
                              ),
                              onSubmitted: (_) => _searchSecurities(),
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: TextField(
                              controller: _indexController,
                              style: theme.textTheme.bodyMedium,
                              decoration: InputDecoration(
                                labelText: 'Index (e.g., NIFTY 50)',
                                labelStyle: TextStyle(color: theme.hintColor),
                                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                                prefixIcon: Icon(Icons.list_alt, color: theme.iconTheme.color?.withOpacity(0.6)),
                                filled: true,
                                fillColor: theme.cardColor,
                              ),
                              onSubmitted: (_) => _searchSecurities(),
                            ),
                          ),
                          const SizedBox(width: 16),
                          ElevatedButton(
                            onPressed: _isLoading ? null : _searchSecurities,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: theme.primaryColor, // Use primary color
                              foregroundColor: Colors.white,
                              padding: const EdgeInsets.symmetric(horizontal: 40, vertical: 20),
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                            ),
                            child: _isLoading 
                              ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                              : const Text('Search', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                          ),
                        ],
                      ),
                    ],
                 ),
              ),
              const SizedBox(height: 16),
  
              // Error Message
              if (_errorMessage.isNotEmpty)
                Container(
                  margin: const EdgeInsets.only(bottom: 16),
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(color: theme.colorScheme.error.withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
                  child: Text(_errorMessage, style: TextStyle(color: theme.colorScheme.error)),
                ),
  
              // Results Grid
              Expanded(
                child: _securities.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.search_off, size: 64, color: theme.disabledColor),
                            const SizedBox(height: 16),
                            Text('No securities found. Try adjusting filters.', style: TextStyle(color: theme.disabledColor)),
                          ],
                        ),
                      )
                    : GridView.builder(
                        gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                          maxCrossAxisExtent: 350,
                          childAspectRatio: 1.8,
                          crossAxisSpacing: 16,
                          mainAxisSpacing: 16,
                        ),
                        itemCount: _securities.length,
                        itemBuilder: (context, index) {
                          final sec = _securities[index];
                          final key = sec['key'] ?? {};
                          final metadata = sec['metadata'] ?? {};
                          final symbol = key['symbol'] ?? 'Unknown';
                          final isin = key['isin'] ?? '-';
                          final sector = metadata['sector'] ?? 'Unknown Sector';
                          final industry = metadata['industry'] ?? 'Unknown Industry';
                          final capType = metadata['market_cap_type'];
                          
                          return Container(
                             decoration: BoxDecoration(
                               color: theme.cardColor,
                               borderRadius: BorderRadius.circular(16),
                               boxShadow: [
                                 BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 10, offset: const Offset(0, 4)),
                               ]
                             ),
                             padding: const EdgeInsets.all(16.0),
                             child: Column(
                               crossAxisAlignment: CrossAxisAlignment.start,
                               mainAxisAlignment: MainAxisAlignment.spaceBetween,
                               children: [
                                 Row(
                                   mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                   children: [
                                     Expanded(
                                       child: Text(
                                         symbol,
                                         style: theme.textTheme.titleMedium?.copyWith(
                                           fontWeight: FontWeight.bold,
                                           fontSize: 20
                                         ),
                                         overflow: TextOverflow.ellipsis,
                                       ),
                                     ),
                                     if (capType != null)
                                       Container(
                                         padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                         decoration: BoxDecoration(
                                           color: _getCapColor(capType).withOpacity(0.1),
                                           borderRadius: BorderRadius.circular(8),
                                           border: Border.all(color: _getCapColor(capType), width: 1),
                                         ),
                                         child: Text(
                                           capType,
                                           style: TextStyle(
                                             color: _getCapColor(capType),
                                             fontSize: 10,
                                             fontWeight: FontWeight.bold,
                                           ),
                                         ),
                                       ),
                                   ],
                                 ),
                                 Divider(color: theme.dividerColor),
                                 Column(
                                   crossAxisAlignment: CrossAxisAlignment.start,
                                   children: [
                                     _buildInfoRow(context, Icons.pie_chart, sector),
                                     const SizedBox(height: 4),
                                     _buildInfoRow(context, Icons.business, industry),
                                     const SizedBox(height: 4),
                                     _buildInfoRow(context, Icons.fingerprint, isin),
                                   ],
                                 ),
                               ],
                             ),
                          );
                        },
                      ),
              ),
            ],
          ),
        ),
      );
  }

  Widget _buildInfoRow(BuildContext context, IconData icon, String text) {
    final theme = Theme.of(context);
    return Row(
      children: [
        Icon(icon, size: 14, color: theme.iconTheme.color?.withOpacity(0.6)),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: theme.textTheme.bodySmall?.copyWith(fontSize: 13),
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }
}
