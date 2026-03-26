# HistoricalDataResponseV1


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**data** | [**Dict[str, HistoricalData]**](HistoricalData.md) |  | [optional] 
**metadata** | [**HistoricalDataMetadata**](HistoricalDataMetadata.md) |  | [optional] 
**error** | **str** |  | [optional] 
**message** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.historical_data_response_v1 import HistoricalDataResponseV1

# TODO update the JSON string below
json = "{}"
# create an instance of HistoricalDataResponseV1 from a JSON string
historical_data_response_v1_instance = HistoricalDataResponseV1.from_json(json)
# print the JSON string representation of the object
print(HistoricalDataResponseV1.to_json())

# convert the object into a dict
historical_data_response_v1_dict = historical_data_response_v1_instance.to_dict()
# create an instance of HistoricalDataResponseV1 from a dict
historical_data_response_v1_from_dict = HistoricalDataResponseV1.from_dict(historical_data_response_v1_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


