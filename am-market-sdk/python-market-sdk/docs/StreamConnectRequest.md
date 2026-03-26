# StreamConnectRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**instrument_keys** | **List[str]** |  | [optional] 
**mode** | **str** |  | [optional] 
**expand_indices** | **bool** |  | [optional] 
**time_frame** | **str** |  | [optional] 
**is_index_symbol** | **bool** |  | [optional] 
**stream** | **bool** |  | [optional] 
**provider** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.stream_connect_request import StreamConnectRequest

# TODO update the JSON string below
json = "{}"
# create an instance of StreamConnectRequest from a JSON string
stream_connect_request_instance = StreamConnectRequest.from_json(json)
# print the JSON string representation of the object
print(StreamConnectRequest.to_json())

# convert the object into a dict
stream_connect_request_dict = stream_connect_request_instance.to_dict()
# create an instance of StreamConnectRequest from a dict
stream_connect_request_from_dict = StreamConnectRequest.from_dict(stream_connect_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


