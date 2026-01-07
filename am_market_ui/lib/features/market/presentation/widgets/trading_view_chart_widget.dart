import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import '../../internal/domain/models/chart_config.dart';

class TradingViewChartWidget extends StatefulWidget {
  const TradingViewChartWidget({
    required this.config,
    super.key,
    this.onChartLoaded,
  });
  final ChartConfig config;
  final VoidCallback? onChartLoaded;

  @override
  State<TradingViewChartWidget> createState() => _TradingViewChartWidgetState();
}

class _TradingViewChartWidgetState extends State<TradingViewChartWidget> {
  late final WebViewController _controller;

  @override
  void initState() {
    super.initState();
    _controller = WebViewController();

    // setJavaScriptMode is not supported on Web (JS is always on).
    // setBackgroundColor is also not fully supported on Web in the same way.
    if (!kIsWeb) {
      _controller
        ..setJavaScriptMode(JavaScriptMode.unrestricted)
        ..setBackgroundColor(const Color(0x00000000));
    }

    if (!kIsWeb) {
      _controller.setNavigationDelegate(
        NavigationDelegate(
          onProgress: (progress) {
            // Update loading bar.
          },
          onPageStarted: (url) {},
          onPageFinished: (url) {
            widget.onChartLoaded?.call();
          },
          onWebResourceError: (error) {},
        ),
      );
    }

    // On Web, the NavigationDelegate is not reliable/supported for local HTML string loading
    // in the same way. We simulate a load complete event or use a small delay.
    if (kIsWeb) {
      // Simulate finish load after a short delay since we can't observe onPageFinished reliably
      Future.delayed(const Duration(milliseconds: 1500), () {
        if (mounted) {
          widget.onChartLoaded?.call();
        }
      });
    }

    _controller.loadHtmlString(_buildHtml(widget.config));
  }

  @override
  void didUpdateWidget(covariant TradingViewChartWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.config != oldWidget.config) {
      _controller.loadHtmlString(_buildHtml(widget.config));
    }
  }

  String _buildHtml(ChartConfig config) =>
      '''
      <!DOCTYPE html>
      <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          body, html { margin: 0; padding: 0; height: 100vh; width: 100vw; overflow: hidden; }
          .tradingview-widget-container { height: 100%; width: 100%; }
          #tradingview_widget { height: 100%; width: 100%; }
        </style>
      </head>
      <body>
        <div class="tradingview-widget-container">
          <div id="tradingview_widget"></div>
          <script type="text/javascript" src="https://s3.tradingview.com/tv.js"></script>
          <script type="text/javascript">
          new TradingView.widget(
          {
            "autosize": true,
            "width": "100%",
            "height": "100%",
            "symbol": "${config.symbol}",
            "interval": "${config.interval}",
            "timezone": "Etc/UTC",
            "theme": "${config.theme}",
            "style": "1",
            "locale": "${config.locale}",
            "toolbar_bg": "#f1f3f6",
            "enable_publishing": false,
            "allow_symbol_change": true,
            "container_id": "tradingview_widget"
          }
          );
          </script>
        </div>
      </body>
      </html>
    ''';

  @override
  Widget build(BuildContext context) {
    // Explicitly force the WebView to fill all available space.
    return SizedBox.expand(child: WebViewWidget(controller: _controller));
  }
}
