import 'package:test/test.dart';
import 'package:am_market_sdk/am_market_sdk.dart';

void main() {
  group('am_market_sdk smoke tests', () {
    test('default API clients are initialized', () {
      // Verify the library can be imported and default API clients are available.
      // This is a generated SDK — comprehensive unit tests live in integration suites.
      expect(marketApiClient, isNotNull);
      expect(parserApiClient, isNotNull);
    });
  });
}

