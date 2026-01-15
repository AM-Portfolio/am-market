import 'package:flutter/material.dart';
import 'dart:async';
import 'package:provider/provider.dart' as provider_pkg; // Aliased to avoid conflict
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:am_design_system/am_design_system.dart';
import 'package:am_market_common/providers/market_provider.dart';
import 'package:am_market_common/models/market_data.dart';
import 'package:am_market_ui/features/market_analysis/providers/market_analysis_providers.dart';

/// V2 Glassmorphic Indices Performance View with Architecture Cards
class IndicesPerformanceViewV2 extends ConsumerStatefulWidget {
  const IndicesPerformanceViewV2({Key? key}) : super(key: key);

  @override
  ConsumerState<IndicesPerformanceViewV2> createState() => _IndicesPerformanceViewV2State();
}

class _IndicesPerformanceViewV2State extends ConsumerState<IndicesPerformanceViewV2> {

  Color _getColorSchemeForChange(double pChange) {
    if (pChange >= 2.0) return const Color(0xFF00B894); // Strong Green
    if (pChange >= 0) return const Color(0xFF00D2D3); // Cyan
    if (pChange >= -2.0) return const Color(0xFFFF9F43); // Orange
    return const Color(0xFFFF6B6B); // Red
  }

  // String _getColorSchemeName(double pChange) ... (Not used in build? kept if needed or removed if unused)

  @override
  Widget build(BuildContext context) {
    // Listen to real-time updates and bridge to MarketProvider
    ref.listen(marketDataStreamProvider, (previous, next) {
        next.whenData((update) {
            final mp = provider_pkg.Provider.of<MarketProvider>(context, listen: false);
            
            // Convert to Map<String, dynamic> for batch processing
            final Map<String, dynamic> batchData = {};
            
            update.quotes.forEach((symbol, quote) {
                batchData[symbol] = {
                    'symbol': symbol,
                    'lastPrice': quote.lastPrice,
                    'change': quote.change,
                    'changePercent': quote.changePercent,
                    'timestamp': update.timestamp
                };
            });
            
            // Send batch to provider for efficient logging/processing
            mp.updateLivePriceBatch(batchData);
        });
    });

    return provider_pkg.Consumer<MarketProvider>(
      builder: (context, provider, child) {
        if (provider.isLoading) {
          return const Center(child: CircularProgressIndicator());
        }

        if (provider.error != null) {
          return Center(
            child: Text(
              'Error: ${provider.error}',
              style: const TextStyle(color: Colors.red),
            ),
          );
        }

        if (provider.allIndicesData.isEmpty) {
          return const Center(
            child: Text(
              'No data loaded',
              style: TextStyle(color: Colors.white70),
            ),
          );
        }

        final allIndices = List<StockIndicesMarketData>.from(provider.allIndicesData);
        allIndices.sort((a, b) => b.pChange.compareTo(a.pChange));
        final isDark = Theme.of(context).brightness == Brightness.dark;

        return Container(
          decoration: AppGlassmorphismV2.techBackground(isDark: isDark),
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header
                InfoLayerCard(
                  title: 'Market Overview',
                  subtitle: 'Real-time indices performance',
                  icon: Icons.dashboard,
                  colorScheme: 'primary',
                ),

                const SizedBox(height: 32),

                // Top Performing Indices
                Text(
                  'TOP PERFORMERS',
                  style: TextStyle(
                    color: isDark ? Colors.white38 : Colors.black45,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 2,
                  ),
                ),
                const SizedBox(height: 16),

                // Auto-Scrolling Ticker
                SizedBox(
                  height: 140, // Height for compact cards
                  child: _AutoScrollingTicker(
                    indices: allIndices, 
                    isDark: isDark,
                    onTap: (symbol) => provider.selectIndex(symbol),
                  ),
                ),

                const SizedBox(height: 40),

                // All Indices
                Text(
                  'ALL INDICES',
                  style: TextStyle(
                    color: isDark ? Colors.white38 : Colors.black45,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    letterSpacing: 2,
                  ),
                ),
                const SizedBox(height: 16),

                GridView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                    maxCrossAxisExtent: 300,
                    childAspectRatio: 1.5,
                    crossAxisSpacing: 16,
                    mainAxisSpacing: 16,
                  ),
                  itemCount: allIndices.length,
                  itemBuilder: (context, index) {
                    final data = allIndices[index];
                    final isPositive = data.pChange >= 0;
                    
                    // Cycle schemes for visual variety in White Mode
                    final schemes = ['primary', 'accent', 'neutral', 'info', 'success'];
                    final scheme = schemes[index % schemes.length];
                    final colors = AppGlassmorphismV2.colorSchemes[scheme]!;

                    return GlassCard(
                      padding: const EdgeInsets.all(16),
                      // Use scheme for V2 styling (pastel fill in white mode)
                      colorScheme: scheme, 
                      onTap: () => provider.selectIndex(data.indexSymbol),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Text(
                                  data.indexSymbol,
                                  style: TextStyle(
                                    color: isDark ? Colors.white : Colors.black87,
                                    fontSize: 13,
                                    fontWeight: FontWeight.bold,
                                  ),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              Icon(
                                isPositive ? Icons.arrow_upward : Icons.arrow_downward,
                                color: isDark ? _getColorSchemeForChange(data.pChange) : colors[0],
                                size: 16,
                              ),
                            ],
                          ),
                          Text(
                            data.lastPrice.toStringAsFixed(2),
                            style: TextStyle(
                              color: isDark ? Colors.white : Colors.black87,
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          Text(
                            '${isPositive ? '+' : ''}${data.pChange.toStringAsFixed(2)}%',
                            style: TextStyle(
                              color: isDark ? _getColorSchemeForChange(data.pChange) : colors[0],
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _AutoScrollingTicker extends StatefulWidget {
  final List<StockIndicesMarketData> indices;
  final bool isDark;
  final Function(String) onTap;

  const _AutoScrollingTicker({
    required this.indices,
    required this.isDark,
    required this.onTap,
  });

  @override
  State<_AutoScrollingTicker> createState() => _AutoScrollingTickerState();
}

class _AutoScrollingTickerState extends State<_AutoScrollingTicker> {
  late ScrollController _scrollController;
  Timer? _timer;
  bool _isScrolling = true;

  @override
  void initState() {
    super.initState();
    _scrollController = ScrollController();
    _startScrolling();
  }

  void _startScrolling() {
    if (!_isScrolling || widget.indices.isEmpty) return;
    
    // Smooth scrolling
    // Wait for build
    Future.delayed(const Duration(milliseconds: 500), () {
      if (!mounted) return;
      _timer = Timer.periodic(const Duration(milliseconds: 50), (timer) {
        if (!mounted || !_isScrolling || !_scrollController.hasClients) {
           timer.cancel();
           return;
        }
        
        // Calculate max scroll extent
        final maxScroll = _scrollController.position.maxScrollExtent;
        final currentScroll = _scrollController.offset;
        
        // Move 1 pixel
        double nextScroll = currentScroll + 1.0;
        
        if (nextScroll >= maxScroll) {
          // Reset to 0 (or jump if infinite list simulation desired, but jump to 0 is ok for now)
          _scrollController.jumpTo(0);
        } else {
          _scrollController.jumpTo(nextScroll);
        }
      });
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _scrollController.dispose();
    super.dispose();
  }

  Color _getColorSchemeForChange(double pChange) {
    if (pChange >= 2.0) return const Color(0xFF00B894); 
    if (pChange >= 0) return const Color(0xFF00D2D3); 
    if (pChange >= -2.0) return const Color(0xFFFF9F43); 
    return const Color(0xFFFF6B6B); 
  }

  @override
  Widget build(BuildContext context) {
    // Duplicate the list to create infinite scroll illusion (x3 list)
    final displayList = [...widget.indices, ...widget.indices, ...widget.indices];

    return GestureDetector(
      onTapDown: (_) => setState(() => _isScrolling = false),
      onTapUp: (_) => setState(() {
         _isScrolling = true;
         _startScrolling();
      }),
      onTapCancel: () => setState(() {
         _isScrolling = true;
         _startScrolling();
      }),
      child: ListView.builder(
        controller: _scrollController,
        scrollDirection: Axis.horizontal,
        itemCount: displayList.length,
        itemBuilder: (context, index) {
          final data = displayList[index];
          // Cycle schemes
          final schemes = ['primary', 'accent', 'neutral', 'info', 'success'];
          // Use original index to keep scheme consistent for same item
          final originalIndex = index % widget.indices.length; 
          final scheme = schemes[originalIndex % schemes.length];
          final colors = AppGlassmorphismV2.colorSchemes[scheme]!;
          
          return Container(
            width: 280, // Fixed width card
            margin: const EdgeInsets.only(right: 16),
            child: MetricCard(
              label: data.indexSymbol,
              value: data.lastPrice.toStringAsFixed(2),
              icon: data.pChange >= 0 ? Icons.trending_up : Icons.trending_down,
              accentColor: widget.isDark ? _getColorSchemeForChange(data.pChange) : colors[0],
              trailing: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: (widget.isDark ? _getColorSchemeForChange(data.pChange) : colors[0]).withOpacity(0.2),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  '${data.pChange >= 0 ? '+' : ''}${data.pChange.toStringAsFixed(2)}%',
                  style: TextStyle(
                    color: widget.isDark ? _getColorSchemeForChange(data.pChange) : colors[0],
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              onTap: () => widget.onTap(data.indexSymbol),
            ),
          );
        },
      ),
    );
  }
}
