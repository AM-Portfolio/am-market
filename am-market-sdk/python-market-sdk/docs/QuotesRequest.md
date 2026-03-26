# QuotesRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**symbols** | **str** |  | [optional] 
**time_frame** | **str** |  | [optional] 
**force_refresh** | **bool** |  | [optional] 
**index_symbol** | **bool** |  | [optional] 

## Example

```python
from am_market_client.models.quotes_request import QuotesRequest

# TODO update the JSON string below
json = "{}"
# create an instance of QuotesRequest from a JSON string
quotes_request_instance = QuotesRequest.from_json(json)
# print the JSON string representation of the object
print(QuotesRequest.to_json())

# convert the object into a dict
quotes_request_dict = quotes_request_instance.to_dict()
# create an instance of QuotesRequest from a dict
quotes_request_from_dict = QuotesRequest.from_dict(quotes_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


