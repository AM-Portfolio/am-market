# am_parser_client.model.FileUpload

## Load the model package
```dart
import 'package:am_parser_client/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** |  | [optional] 
**fileId** | **String** | Unique identifier for the file | 
**originalFilename** | **String** | Original filename uploaded by user | 
**storedFilename** | **String** | Filename as stored in the system | 
**fileType** | [**FileType**](FileType.md) | Type of file (excel, sheet, csv) | 
**filePath** | **String** | Full path to stored file | 
**parentId** | **String** |  | [optional] 
**sheetName** | **String** |  | [optional] 
**status** | [**ProcessingStatus**](ProcessingStatus.md) | Current processing status | [optional] [default to ProcessingStatus.uploaded]
**fileSize** | **int** | File size in bytes | 
**createdAt** | [**DateTime**](DateTime.md) |  | [optional] 
**updatedAt** | [**DateTime**](DateTime.md) |  | [optional] 
**errorMessage** | **String** |  | [optional] 
**processingMetadata** | [**Object**](.md) |  | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


