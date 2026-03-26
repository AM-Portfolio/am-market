# FileListResponse

Response model for listing files

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**files** | [**List[FileUpload]**](FileUpload.md) |  | 
**total_count** | **int** |  | 

## Example

```python
from am_parser_client.models.file_list_response import FileListResponse

# TODO update the JSON string below
json = "{}"
# create an instance of FileListResponse from a JSON string
file_list_response_instance = FileListResponse.from_json(json)
# print the JSON string representation of the object
print(FileListResponse.to_json())

# convert the object into a dict
file_list_response_dict = file_list_response_instance.to_dict()
# create an instance of FileListResponse from a dict
file_list_response_from_dict = FileListResponse.from_dict(file_list_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


