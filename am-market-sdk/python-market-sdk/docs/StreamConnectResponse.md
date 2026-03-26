# StreamConnectResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**status** | **str** |  | [optional] 
**message** | **str** |  | [optional] 
**data** | [**MarketDataUpdate**](MarketDataUpdate.md) |  | [optional] 

## Example

```python
from am_market_client.models.stream_connect_response import StreamConnectResponse

# TODO update the JSON string below
json = "{}"
# create an instance of StreamConnectResponse from a JSON string
stream_connect_response_instance = StreamConnectResponse.from_json(json)
# print the JSON string representation of the object
print(StreamConnectResponse.to_json())

# convert the object into a dict
stream_connect_response_dict = stream_connect_response_instance.to_dict()
# create an instance of StreamConnectResponse from a dict
stream_connect_response_from_dict = StreamConnectResponse.from_dict(stream_connect_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


