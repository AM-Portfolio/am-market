# QuoteChange


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**last_price** | **float** |  | [optional] 
**open** | **float** |  | [optional] 
**high** | **float** |  | [optional] 
**low** | **float** |  | [optional] 
**close** | **float** |  | [optional] 
**previous_close** | **float** |  | [optional] 
**change** | **float** |  | [optional] 
**change_percent** | **float** |  | [optional] 

## Example

```python
from am_market_client.models.quote_change import QuoteChange

# TODO update the JSON string below
json = "{}"
# create an instance of QuoteChange from a JSON string
quote_change_instance = QuoteChange.from_json(json)
# print the JSON string representation of the object
print(QuoteChange.to_json())

# convert the object into a dict
quote_change_dict = quote_change_instance.to_dict()
# create an instance of QuoteChange from a dict
quote_change_from_dict = QuoteChange.from_dict(quote_change_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


