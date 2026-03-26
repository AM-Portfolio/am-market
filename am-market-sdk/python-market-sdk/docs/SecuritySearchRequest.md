# SecuritySearchRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**symbols** | **List[str]** |  | [optional] 
**isin** | **str** |  | [optional] 
**sector** | **str** |  | [optional] 
**industry** | **str** |  | [optional] 
**index** | **str** |  | [optional] 
**query** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.security_search_request import SecuritySearchRequest

# TODO update the JSON string below
json = "{}"
# create an instance of SecuritySearchRequest from a JSON string
security_search_request_instance = SecuritySearchRequest.from_json(json)
# print the JSON string representation of the object
print(SecuritySearchRequest.to_json())

# convert the object into a dict
security_search_request_dict = security_search_request_instance.to_dict()
# create an instance of SecuritySearchRequest from a dict
security_search_request_from_dict = SecuritySearchRequest.from_dict(security_search_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


