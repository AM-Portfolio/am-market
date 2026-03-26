# MarginCalculationRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**positions** | [**List[Position]**](Position.md) |  | [optional] 
**order_type** | **str** |  | [optional] 
**exchange** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.margin_calculation_request import MarginCalculationRequest

# TODO update the JSON string below
json = "{}"
# create an instance of MarginCalculationRequest from a JSON string
margin_calculation_request_instance = MarginCalculationRequest.from_json(json)
# print the JSON string representation of the object
print(MarginCalculationRequest.to_json())

# convert the object into a dict
margin_calculation_request_dict = margin_calculation_request_instance.to_dict()
# create an instance of MarginCalculationRequest from a dict
margin_calculation_request_from_dict = MarginCalculationRequest.from_dict(margin_calculation_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


