# OHLCRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**symbols** | **str** |  | [optional] 
**is_index_symbol** | **bool** |  | [optional] 
**time_frame** | **str** |  | [optional] 
**refresh** | **bool** |  | [optional] 

## Example

```python
from am_market_client.models.ohlc_request import OHLCRequest

# TODO update the JSON string below
json = "{}"
# create an instance of OHLCRequest from a JSON string
ohlc_request_instance = OHLCRequest.from_json(json)
# print the JSON string representation of the object
print(OHLCRequest.to_json())

# convert the object into a dict
ohlc_request_dict = ohlc_request_instance.to_dict()
# create an instance of OHLCRequest from a dict
ohlc_request_from_dict = OHLCRequest.from_dict(ohlc_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


