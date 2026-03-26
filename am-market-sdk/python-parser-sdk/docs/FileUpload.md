# FileUpload

Model for uploaded files and their processing status

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **str** |  | [optional] 
**file_id** | **str** | Unique identifier for the file | 
**original_filename** | **str** | Original filename uploaded by user | 
**stored_filename** | **str** | Filename as stored in the system | 
**file_type** | [**FileType**](FileType.md) | Type of file (excel, sheet, csv) | 
**file_path** | **str** | Full path to stored file | 
**parent_id** | **str** |  | [optional] 
**sheet_name** | **str** |  | [optional] 
**status** | [**ProcessingStatus**](ProcessingStatus.md) | Current processing status | [optional] 
**file_size** | **int** | File size in bytes | 
**created_at** | **datetime** |  | [optional] 
**updated_at** | **datetime** |  | [optional] 
**error_message** | **str** |  | [optional] 
**processing_metadata** | **object** |  | [optional] 

## Example

```python
from am_parser_client.models.file_upload import FileUpload

# TODO update the JSON string below
json = "{}"
# create an instance of FileUpload from a JSON string
file_upload_instance = FileUpload.from_json(json)
# print the JSON string representation of the object
print(FileUpload.to_json())

# convert the object into a dict
file_upload_dict = file_upload_instance.to_dict()
# create an instance of FileUpload from a dict
file_upload_from_dict = FileUpload.from_dict(file_upload_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


