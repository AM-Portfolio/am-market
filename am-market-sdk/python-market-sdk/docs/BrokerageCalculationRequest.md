# BrokerageCalculationRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**trading_symbol** | **str** |  | [optional] 
**quantity** | **int** |  | [optional] 
**buy_price** | **float** |  | [optional] 
**sell_price** | **float** |  | [optional] 
**exchange** | **str** |  | [optional] 
**trade_type** | **str** |  | [optional] 
**broker_type** | **str** |  | [optional] 
**broker_name** | **str** |  | [optional] 
**broker_flat_fee** | **float** |  | [optional] 
**broker_percentage_fee** | **float** |  | [optional] 
**state_code** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.brokerage_calculation_request import BrokerageCalculationRequest

# TODO update the JSON string below
json = "{}"
# create an instance of BrokerageCalculationRequest from a JSON string
brokerage_calculation_request_instance = BrokerageCalculationRequest.from_json(json)
# print the JSON string representation of the object
print(BrokerageCalculationRequest.to_json())

# convert the object into a dict
brokerage_calculation_request_dict = brokerage_calculation_request_instance.to_dict()
# create an instance of BrokerageCalculationRequest from a dict
brokerage_calculation_request_from_dict = BrokerageCalculationRequest.from_dict(brokerage_calculation_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


