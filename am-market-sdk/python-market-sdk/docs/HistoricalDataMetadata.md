# HistoricalDataMetadata


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**from_date** | **str** |  | [optional] 
**to_date** | **str** |  | [optional] 
**interval** | **str** |  | [optional] 
**interval_enum** | **str** |  | [optional] 
**total_symbols** | **int** |  | [optional] 
**successful_symbols** | **int** |  | [optional] 
**total_data_points** | **int** |  | [optional] 
**filtered_data_points** | **int** |  | [optional] 
**filtered** | **bool** |  | [optional] 
**filter_type** | **str** |  | [optional] 
**filter_frequency** | **int** |  | [optional] 
**processing_time_ms** | **int** |  | [optional] 
**source** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.historical_data_metadata import HistoricalDataMetadata

# TODO update the JSON string below
json = "{}"
# create an instance of HistoricalDataMetadata from a JSON string
historical_data_metadata_instance = HistoricalDataMetadata.from_json(json)
# print the JSON string representation of the object
print(HistoricalDataMetadata.to_json())

# convert the object into a dict
historical_data_metadata_dict = historical_data_metadata_instance.to_dict()
# create an instance of HistoricalDataMetadata from a dict
historical_data_metadata_from_dict = HistoricalDataMetadata.from_dict(historical_data_metadata_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


