# StockData


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**symbol** | **str** |  | [optional] 
**identifier** | **str** |  | [optional] 
**series** | **str** |  | [optional] 
**name** | **str** |  | [optional] 
**ffmc** | **int** |  | [optional] 
**company_name** | **str** |  | [optional] 
**isin** | **str** |  | [optional] 
**industry** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.stock_data import StockData

# TODO update the JSON string below
json = "{}"
# create an instance of StockData from a JSON string
stock_data_instance = StockData.from_json(json)
# print the JSON string representation of the object
print(StockData.to_json())

# convert the object into a dict
stock_data_dict = stock_data_instance.to_dict()
# create an instance of StockData from a dict
stock_data_from_dict = StockData.from_dict(stock_data_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


