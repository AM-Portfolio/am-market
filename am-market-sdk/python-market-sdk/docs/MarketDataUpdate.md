# MarketDataUpdate


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**timestamp** | **int** |  | [optional] 
**quotes** | [**Dict[str, QuoteChange]**](QuoteChange.md) |  | [optional] 

## Example

```python
from am_market_client.models.market_data_update import MarketDataUpdate

# TODO update the JSON string below
json = "{}"
# create an instance of MarketDataUpdate from a JSON string
market_data_update_instance = MarketDataUpdate.from_json(json)
# print the JSON string representation of the object
print(MarketDataUpdate.to_json())

# convert the object into a dict
market_data_update_dict = market_data_update_instance.to_dict()
# create an instance of MarketDataUpdate from a dict
market_data_update_from_dict = MarketDataUpdate.from_dict(market_data_update_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


