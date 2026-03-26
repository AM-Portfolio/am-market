# SecurityMetadata


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**sector** | **str** |  | [optional] 
**industry** | **str** |  | [optional] 
**market_cap_value** | **int** |  | [optional] 
**market_cap_type** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.security_metadata import SecurityMetadata

# TODO update the JSON string below
json = "{}"
# create an instance of SecurityMetadata from a JSON string
security_metadata_instance = SecurityMetadata.from_json(json)
# print the JSON string representation of the object
print(SecurityMetadata.to_json())

# convert the object into a dict
security_metadata_dict = security_metadata_instance.to_dict()
# create an instance of SecurityMetadata from a dict
security_metadata_from_dict = SecurityMetadata.from_dict(security_metadata_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


