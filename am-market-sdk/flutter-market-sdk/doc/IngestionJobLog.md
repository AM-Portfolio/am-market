# am_market_client.model.IngestionJobLog

## Load the model package
```dart
import 'package:am_market_client/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** |  | [optional] 
**jobId** | **String** |  | [optional] 
**startTime** | [**DateTime**](DateTime.md) |  | [optional] 
**endTime** | [**DateTime**](DateTime.md) |  | [optional] 
**status** | **String** |  | [optional] 
**totalSymbols** | **int** |  | [optional] 
**successCount** | **int** |  | [optional] 
**failureCount** | **int** |  | [optional] 
**failedSymbols** | **List<String>** |  | [optional] [default to const []]
**durationMs** | **int** |  | [optional] 
**payloadSize** | **int** |  | [optional] 
**message** | **String** |  | [optional] 
**logs** | **List<String>** |  | [optional] [default to const []]

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


