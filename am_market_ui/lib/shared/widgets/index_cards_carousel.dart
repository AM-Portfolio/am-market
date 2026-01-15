import 'package:flutter/material.dart';
import 'package:am_market_common/models/market_data.dart';
import 'package:am_market_ui/shared/widgets/index_card.dart';

/// Horizontal scrolling carousel of index cards
class IndexCardsCarousel extends StatelessWidget {
  final List<StockIndicesMarketData> indices;

  const IndexCardsCarousel({
    required this.indices,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    if (indices.isEmpty) {
      return const SizedBox.shrink();
    }

    return Container(
      height: 140,
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: indices.length,
        itemBuilder: (context, index) {
          return IndexCard(data: indices[index]);
        },
      ),
    );
  }
}
