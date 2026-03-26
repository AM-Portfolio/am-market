# MarginCalculationResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**total_margin_required** | **float** |  | [optional] 
**span_margin** | **float** |  | [optional] 
**exposure_margin** | **float** |  | [optional] 
**additional_margin** | **float** |  | [optional] 
**position_margins** | [**Dict[str, PositionMargin]**](PositionMargin.md) |  | [optional] 
**status** | **str** |  | [optional] 
**error** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.margin_calculation_response import MarginCalculationResponse

# TODO update the JSON string below
json = "{}"
# create an instance of MarginCalculationResponse from a JSON string
margin_calculation_response_instance = MarginCalculationResponse.from_json(json)
# print the JSON string representation of the object
print(MarginCalculationResponse.to_json())

# convert the object into a dict
margin_calculation_response_dict = margin_calculation_response_instance.to_dict()
# create an instance of MarginCalculationResponse from a dict
margin_calculation_response_from_dict = MarginCalculationResponse.from_dict(margin_calculation_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


