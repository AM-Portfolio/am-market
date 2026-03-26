

# FileUpload

Model for uploaded files and their processing status

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**id** | **String** |  |  [optional] |
|**fileId** | **String** | Unique identifier for the file |  |
|**originalFilename** | **String** | Original filename uploaded by user |  |
|**storedFilename** | **String** | Filename as stored in the system |  |
|**fileType** | **FileType** | Type of file (excel, sheet, csv) |  |
|**filePath** | **String** | Full path to stored file |  |
|**parentId** | **String** |  |  [optional] |
|**sheetName** | **String** |  |  [optional] |
|**status** | **ProcessingStatus** | Current processing status |  [optional] |
|**fileSize** | **Integer** | File size in bytes |  |
|**createdAt** | **OffsetDateTime** |  |  [optional] |
|**updatedAt** | **OffsetDateTime** |  |  [optional] |
|**errorMessage** | **String** |  |  [optional] |
|**processingMetadata** | **Object** |  |  [optional] |



