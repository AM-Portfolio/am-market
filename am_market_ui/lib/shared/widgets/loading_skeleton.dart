import 'package:flutter/material.dart';
import 'package:am_design_system/am_design_system.dart';

/// Loading skeleton for indices list
class IndicesLoadingSkeleton extends StatelessWidget {
  const IndicesLoadingSkeleton({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ShimmerLoading(
      child: ListView.builder(
        itemCount: 6,
        padding: const EdgeInsets.all(8),
        itemBuilder: (context, index) {
          return Card(
            margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
            child: ListTile(
              leading: SkeletonBox(width: 40, height: 40, borderRadius: BorderRadius.circular(8)),
              title: const SkeletonLine(width: double.infinity, height: 16),
              subtitle: const Padding(
                padding: EdgeInsets.only(top: 8),
                child: SkeletonLine(width: 100, height: 12),
              ),
              trailing: SkeletonBox(width: 60, height: 20, borderRadius: BorderRadius.circular(4)),
            ),
          );
        },
      ),
    );
  }
}
