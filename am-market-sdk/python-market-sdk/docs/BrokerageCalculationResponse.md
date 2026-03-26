# BrokerageCalculationResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**buy_transaction_value** | **float** |  | [optional] 
**sell_transaction_value** | **float** |  | [optional] 
**buy_brokerage** | **float** |  | [optional] 
**sell_brokerage** | **float** |  | [optional] 
**buy_stt** | **float** |  | [optional] 
**sell_stt** | **float** |  | [optional] 
**buy_exchange_charges** | **float** |  | [optional] 
**sell_exchange_charges** | **float** |  | [optional] 
**buy_gst** | **float** |  | [optional] 
**sell_gst** | **float** |  | [optional] 
**buy_sebi_charges** | **float** |  | [optional] 
**sell_sebi_charges** | **float** |  | [optional] 
**buy_stamp_duty** | **float** |  | [optional] 
**sell_stamp_duty** | **float** |  | [optional] 
**dp_charges** | **float** |  | [optional] 
**total_buy_charges** | **float** |  | [optional] 
**total_sell_charges** | **float** |  | [optional] 
**total_charges** | **float** |  | [optional] 
**net_profit_loss** | **float** |  | [optional] 
**charges_percentage** | **float** |  | [optional] 
**break_even_price** | **float** |  | [optional] 
**status** | **str** |  | [optional] 
**error** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.brokerage_calculation_response import BrokerageCalculationResponse

# TODO update the JSON string below
json = "{}"
# create an instance of BrokerageCalculationResponse from a JSON string
brokerage_calculation_response_instance = BrokerageCalculationResponse.from_json(json)
# print the JSON string representation of the object
print(BrokerageCalculationResponse.to_json())

# convert the object into a dict
brokerage_calculation_response_dict = brokerage_calculation_response_instance.to_dict()
# create an instance of BrokerageCalculationResponse from a dict
brokerage_calculation_response_from_dict = BrokerageCalculationResponse.from_dict(brokerage_calculation_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


