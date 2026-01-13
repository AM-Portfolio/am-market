import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart';

class VerticalScrollNavigator extends StatefulWidget {
  final Widget child;
  final VoidCallback? onNextPage;
  final VoidCallback? onPreviousPage;

  const VerticalScrollNavigator({
    super.key,
    required this.child,
    this.onNextPage,
    this.onPreviousPage,
  });

  @override
  State<VerticalScrollNavigator> createState() => _VerticalScrollNavigatorState();
}

class _VerticalScrollNavigatorState extends State<VerticalScrollNavigator> with AutomaticKeepAliveClientMixin {
  bool _isNavigating = false;
  bool _isAtTop = true;
  bool _isAtBottom = false;

  @override
  bool get wantKeepAlive => true;

  void _handleNavigation(bool isNext) async {
    if (_isNavigating) return;
    if (isNext && widget.onNextPage == null) return;
    if (!isNext && widget.onPreviousPage == null) return;

    setState(() => _isNavigating = true);
    
    if (isNext) {
       widget.onNextPage?.call();
    } else {
       widget.onPreviousPage?.call();
    }

    // Debounce navigation to prevent rapid firing
    await Future.delayed(const Duration(milliseconds: 500));
    if (mounted) setState(() => _isNavigating = false);
  }

  bool _onScrollNotification(ScrollNotification notification) {
    if (notification.metrics.axis != Axis.vertical) return false;

    // Update state tracking
    if (notification is ScrollUpdateNotification) {
      // Use a small tolerance
      final metrics = notification.metrics;
      _isAtTop = metrics.pixels <= metrics.minScrollExtent;
      _isAtBottom = metrics.pixels >= metrics.maxScrollExtent;
    }

    // Handle Overscroll (Mobile/Touch)
    if (notification is OverscrollNotification) {
      if (notification.overscroll > 0 && _isAtBottom) {
        _handleNavigation(true); // Next
      } else if (notification.overscroll < 0 && _isAtTop) {
        _handleNavigation(false); // Previous
      }
    }
    return false;
  }

  void _onPointerScroll(PointerScrollEvent event) {
    // Handle Mouse Wheel (Desktop)
    if (event.scrollDelta.dy > 0 && _isAtBottom) {
      // Scrolling down while at bottom
      _handleNavigation(true);
    } else if (event.scrollDelta.dy < 0 && _isAtTop) {
      // Scrolling up while at top
      _handleNavigation(false);
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    return Listener(
      onPointerSignal: (event) {
        if (event is PointerScrollEvent) {
          _onPointerScroll(event);
        }
      },
      child: NotificationListener<ScrollNotification>(
        onNotification: _onScrollNotification,
        child: widget.child,
      ),
    );
  }
}
