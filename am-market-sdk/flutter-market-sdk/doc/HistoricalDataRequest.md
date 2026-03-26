# am_market_client.model.HistoricalDataRequest

## Load the model package
```dart
import 'package:am_market_client/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**symbols** | **String** |  | [optional] 
**from** | **String** | Start date in yyyy-MM-dd format | [optional] 
**to** | **String** | End date in yyyy-MM-dd format (optional, defaults to current date) | [optional] 
**interval** | **String** |  | [optional] 
**continuous** | **bool** |  | [optional] 
**instrumentType** | **String** |  | [optional] 
**forceRefresh** | **bool** |  | [optional] 
**filterType** | **String** |  | [optional] 
**filterFrequency** | **int** |  | [optional] 
**additionalParams** | [**Map<String, Object>**](Object.md) |  | [optional] [default to const {}]
**isIndexSymbol** | **bool** | Whether the symbols represent indices that should be expanded to constituent stocks | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


