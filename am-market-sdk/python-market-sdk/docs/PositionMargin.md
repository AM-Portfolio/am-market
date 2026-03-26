# PositionMargin


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**trading_symbol** | **str** |  | [optional] 
**total_margin** | **float** |  | [optional] 
**span_margin** | **float** |  | [optional] 
**exposure_margin** | **float** |  | [optional] 
**additional_margin** | **float** |  | [optional] 
**type** | **str** |  | [optional] 
**exchange** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.position_margin import PositionMargin

# TODO update the JSON string below
json = "{}"
# create an instance of PositionMargin from a JSON string
position_margin_instance = PositionMargin.from_json(json)
# print the JSON string representation of the object
print(PositionMargin.to_json())

# convert the object into a dict
position_margin_dict = position_margin_instance.to_dict()
# create an instance of PositionMargin from a dict
position_margin_from_dict = PositionMargin.from_dict(position_margin_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


