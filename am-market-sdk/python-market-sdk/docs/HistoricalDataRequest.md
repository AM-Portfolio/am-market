# HistoricalDataRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**symbols** | **str** |  | [optional] 
**var_from** | **str** | Start date in yyyy-MM-dd format | [optional] 
**to** | **str** | End date in yyyy-MM-dd format (optional, defaults to current date) | [optional] 
**interval** | **str** |  | [optional] 
**continuous** | **bool** |  | [optional] 
**instrument_type** | **str** |  | [optional] 
**force_refresh** | **bool** |  | [optional] 
**filter_type** | **str** |  | [optional] 
**filter_frequency** | **int** |  | [optional] 
**additional_params** | **Dict[str, object]** |  | [optional] 
**is_index_symbol** | **bool** | Whether the symbols represent indices that should be expanded to constituent stocks | [optional] 

## Example

```python
from am_market_client.models.historical_data_request import HistoricalDataRequest

# TODO update the JSON string below
json = "{}"
# create an instance of HistoricalDataRequest from a JSON string
historical_data_request_instance = HistoricalDataRequest.from_json(json)
# print the JSON string representation of the object
print(HistoricalDataRequest.to_json())

# convert the object into a dict
historical_data_request_dict = historical_data_request_instance.to_dict()
# create an instance of HistoricalDataRequest from a dict
historical_data_request_from_dict = HistoricalDataRequest.from_dict(historical_data_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


