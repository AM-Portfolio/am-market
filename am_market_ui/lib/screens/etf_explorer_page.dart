import 'package:flutter/material.dart';
import '../models/etf.dart';
import '../services/etf_service.dart';
import 'etf_detail_page.dart';

class EtfExplorerPage extends StatefulWidget {
  const EtfExplorerPage({super.key});

  @override
  State<EtfExplorerPage> createState() => _EtfExplorerPageState();
}

class _EtfExplorerPageState extends State<EtfExplorerPage> {
  final EtfService _etfService = EtfService();
  final TextEditingController _searchController = TextEditingController();
  List<Etf> _etfs = [];
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    // Removed automatic loading - only load when user searches
    // This prevents unnecessary API calls when page is not active
  }

  Future<void> _loadEtfs(String query) async {
    if (!mounted) return; // Don't load if widget is not mounted
    setState(() => _isLoading = true);
    final results = await _etfService.searchEtfs(query);
    if (mounted) {
      setState(() {
        _etfs = results;
        _isLoading = false;
      });
    }
  }

  // State to track selected ETF for inline display
  Etf? _selectedEtf;

  @override
  Widget build(BuildContext context) {
    
    // If an ETF is selected, show detail view inline
    if (_selectedEtf != null) {
      return EtfDetailPage(
        symbol: _selectedEtf!.symbol, 
        name: _selectedEtf!.name,
        onBack: () {
          setState(() {
            _selectedEtf = null;
          });
        },
      );
    }
    
    final theme = Theme.of(context);
    
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Icon(Icons.dashboard_customize, color: theme.primaryColor, size: 28),
              const SizedBox(width: 10),
              Text(
                "ETF Explorer",
                style: theme.textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: theme.colorScheme.secondary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          // Search Bar
          // Search Bar with Autocomplete
          Autocomplete<Etf>(
            optionsBuilder: (TextEditingValue textEditingValue) async {
              if (textEditingValue.text == '') {
                return const Iterable<Etf>.empty();
              }
              // Debounce could be added here if needed, but for now direct call
              return await _etfService.searchEtfs(textEditingValue.text);
            },
            displayStringForOption: (Etf option) => "${option.symbol} - ${option.name}",
            onSelected: (Etf selection) {
               setState(() {
                 _selectedEtf = selection;
               });
            },
            fieldViewBuilder: (BuildContext context, TextEditingController fieldTextEditingController,
                FocusNode fieldFocusNode, VoidCallback onFieldSubmitted) {
              return TextField(
                controller: fieldTextEditingController,
                focusNode: fieldFocusNode,
                style: theme.textTheme.bodyMedium,
                decoration: InputDecoration(
                  prefixIcon: Icon(Icons.search, color: theme.iconTheme.color),
                  hintText: "Search ETFs (e.g. NIFTYBEES, GOLDBEES)...",
                  hintStyle: theme.textTheme.bodyMedium?.copyWith(color: theme.hintColor),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(10),
                    borderSide: BorderSide(color: theme.dividerColor),
                  ),
                  filled: true,
                  fillColor: theme.cardColor,
                  suffixIcon: fieldTextEditingController.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () {
                          fieldTextEditingController.clear();
                          _loadEtfs("Nifty"); // Reset to default
                        },
                      )
                    : null,
                ),
                onSubmitted: (value) {
                  _loadEtfs(value.isEmpty ? "Nifty" : value);
                },
              );
            },
            optionsViewBuilder: (BuildContext context, AutocompleteOnSelected<Etf> onSelected, Iterable<Etf> options) {
              return Align(
                alignment: Alignment.topLeft,
                child: Material(
                  elevation: 4.0,
                  color: theme.cardColor,
                  child: SizedBox(
                    width: MediaQuery.of(context).size.width - 32, // Match parent padding
                    child: ListView.builder(
                      padding: EdgeInsets.zero,
                      shrinkWrap: true,
                      itemCount: options.length,
                      itemBuilder: (BuildContext context, int index) {
                        final Etf option = options.elementAt(index);
                        return ListTile(
                          title: Text(option.symbol, style: const TextStyle(fontWeight: FontWeight.bold)),
                          subtitle: Text(option.name),
                          onTap: () => onSelected(option),
                        );
                      },
                    ),
                  ),
                ),
              );
            },
          ),
          const SizedBox(height: 20),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _etfs.isEmpty
                    ? Center(child: Text("No ETFs found.", style: theme.textTheme.bodyLarge))
                    : ListView.builder(
                        itemCount: _etfs.length,
                        itemBuilder: (context, index) {
                          final etf = _etfs[index];
                          return Card(
                            margin: const EdgeInsets.only(bottom: 12),
                            elevation: 0,
                            color: theme.cardColor,
                            shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                                side: BorderSide(color: theme.dividerColor.withOpacity(0.5))),
                            child: ListTile(
                              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                              leading: CircleAvatar(
                                backgroundColor: theme.primaryColor.withOpacity(0.1),
                                child: Text(etf.symbol.substring(0, 1),
                                    style: TextStyle(color: theme.primaryColor, fontWeight: FontWeight.bold)),
                              ),
                              title: Text(etf.name,
                                  style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                              subtitle: Text(
                                  "${etf.symbol} • ${etf.assetClass ?? 'N/A'} • ${etf.marketCapCategory ?? 'N/A'}",
                                  style: theme.textTheme.bodySmall),
                              trailing:
                                  Icon(Icons.arrow_forward_ios, size: 14, color: theme.iconTheme.color?.withOpacity(0.5)),
                              onTap: () {
                                setState(() {
                                  _selectedEtf = etf;
                                });
                              },
                            ),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }
}
