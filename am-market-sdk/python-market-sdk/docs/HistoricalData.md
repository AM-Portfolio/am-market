# HistoricalData


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**trading_symbol** | **str** |  | [optional] 
**isin** | **str** |  | [optional] 
**from_date** | **datetime** |  | [optional] 
**to_date** | **datetime** |  | [optional] 
**interval** | **str** |  | [optional] 
**data_points** | [**List[OHLCVTPoint]**](OHLCVTPoint.md) |  | [optional] 
**data_point_count** | **int** |  | [optional] 
**exchange** | **str** |  | [optional] 
**currency** | **str** |  | [optional] 
**retrieval_time** | **datetime** |  | [optional] 

## Example

```python
from am_market_client.models.historical_data import HistoricalData

# TODO update the JSON string below
json = "{}"
# create an instance of HistoricalData from a JSON string
historical_data_instance = HistoricalData.from_json(json)
# print the JSON string representation of the object
print(HistoricalData.to_json())

# convert the object into a dict
historical_data_dict = historical_data_instance.to_dict()
# create an instance of HistoricalData from a dict
historical_data_from_dict = HistoricalData.from_dict(historical_data_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


