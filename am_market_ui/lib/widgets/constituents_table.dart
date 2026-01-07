import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:am_design_system/am_design_system.dart';
import '../providers/market_provider.dart';
import '../screens/stock_detail_page.dart';
import '../models/market_data.dart';

class ConstituentsTable extends StatelessWidget {
  const ConstituentsTable({super.key});

  @override
  Widget build(BuildContext context) {
    // Watch for index data changes
    final provider = context.watch<MarketProvider>();
    final data = provider.currentIndexData;

    if (data == null || data.stocks.isEmpty) {
      return const Center(child: Text('No data available'));
    }

    // SortableTable handles display and sorting
    // We bind the data source and columns here
    return StreamBuilder<Map<String, dynamic>>(
      stream: provider.livePriceStream,
      builder: (context, snapshot) {
        return SortableTable<StockData>(
          items: data.stocks,
          onItemTap: (stock) => _navigateToDetail(context, stock),
          columns: [
            SortableColumn<StockData>(
              title: 'Symbol',
              builder: (stock) => Text(stock.symbol, style: const TextStyle(fontWeight: FontWeight.bold)),
              sortBy: (stock) => stock.symbol,
            ),
            SortableColumn<StockData>(
              title: 'Price',
              builder: (stock) {
                final liveData = provider.livePrices[stock.symbol];
                final price = liveData != null ? (liveData['lastPrice'] as num).toDouble() : stock.lastPrice;
                return Text(
                  price.toStringAsFixed(2),
                  style: TextStyle(
                    fontWeight: liveData != null ? FontWeight.bold : FontWeight.normal,
                    color: liveData != null ? AppColors.info : null,
                  ),
                );
              },
              sortBy: (stock) => stock.lastPrice,
              textAlign: TextAlign.right,
            ),
            SortableColumn<StockData>(
              title: 'Change %',
              builder: (stock) {
                final liveData = provider.livePrices[stock.symbol];
                final pChange = liveData != null ? (liveData['changePercent'] as num).toDouble() : stock.pChange;
                final isPositive = pChange >= 0;
                return Text(
                  '${isPositive ? '+' : ''}${pChange.toStringAsFixed(2)}%',
                  style: TextStyle(
                    color: isPositive ? AppColors.success : AppColors.error,
                    fontWeight: FontWeight.bold,
                  ),
                );
              },
              sortBy: (stock) => stock.pChange,
              textAlign: TextAlign.right,
            ),
            SortableColumn<StockData>(
              title: 'Open',
              builder: (stock) => Text(stock.open.toStringAsFixed(2)),
              sortBy: (stock) => stock.open,
              textAlign: TextAlign.right,
            ),
            SortableColumn<StockData>(
              title: 'High',
              builder: (stock) => Text(stock.dayHigh.toStringAsFixed(2)),
              sortBy: (stock) => stock.dayHigh,
              textAlign: TextAlign.right,
            ),
             SortableColumn<StockData>(
              title: 'Low',
              builder: (stock) => Text(stock.dayLow.toStringAsFixed(2)),
              sortBy: (stock) => stock.dayLow,
              textAlign: TextAlign.right,
            ),
          ],
        );
      },
    );
  }

  void _navigateToDetail(BuildContext context, StockData stock) {
     Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => StockDetailPage(symbol: stock.symbol),
      ),
    );
  }
}
