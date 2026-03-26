# StockIndicesMarketData


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**index_symbol** | **str** |  | [optional] 
**data** | [**List[StockData]**](StockData.md) |  | [optional] 
**metadata** | [**IndexMetadata**](IndexMetadata.md) |  | [optional] 
**doc_version** | **str** |  | [optional] 
**audit** | [**AuditData**](AuditData.md) |  | [optional] 

## Example

```python
from am_market_client.models.stock_indices_market_data import StockIndicesMarketData

# TODO update the JSON string below
json = "{}"
# create an instance of StockIndicesMarketData from a JSON string
stock_indices_market_data_instance = StockIndicesMarketData.from_json(json)
# print the JSON string representation of the object
print(StockIndicesMarketData.to_json())

# convert the object into a dict
stock_indices_market_data_dict = stock_indices_market_data_instance.to_dict()
# create an instance of StockIndicesMarketData from a dict
stock_indices_market_data_from_dict = StockIndicesMarketData.from_dict(stock_indices_market_data_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


