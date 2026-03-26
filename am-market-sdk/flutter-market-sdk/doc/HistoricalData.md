# am_market_client.model.HistoricalData

## Load the model package
```dart
import 'package:am_market_client/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**tradingSymbol** | **String** |  | [optional] 
**isin** | **String** |  | [optional] 
**fromDate** | [**DateTime**](DateTime.md) |  | [optional] 
**toDate** | [**DateTime**](DateTime.md) |  | [optional] 
**interval** | **String** |  | [optional] 
**dataPoints** | [**List<OHLCVTPoint>**](OHLCVTPoint.md) |  | [optional] [default to const []]
**dataPointCount** | **int** |  | [optional] 
**exchange** | **String** |  | [optional] 
**currency** | **String** |  | [optional] 
**retrievalTime** | [**DateTime**](DateTime.md) |  | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


