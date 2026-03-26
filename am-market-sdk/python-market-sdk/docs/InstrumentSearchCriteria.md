# InstrumentSearchCriteria


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**queries** | **List[str]** |  | [optional] 
**exchanges** | **List[str]** |  | [optional] 
**instrument_types** | **List[str]** |  | [optional] 
**segments** | **List[str]** |  | [optional] 
**isins** | **List[str]** |  | [optional] 
**trading_symbols** | **List[str]** |  | [optional] 
**weekly** | **bool** |  | [optional] 
**provider** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.instrument_search_criteria import InstrumentSearchCriteria

# TODO update the JSON string below
json = "{}"
# create an instance of InstrumentSearchCriteria from a JSON string
instrument_search_criteria_instance = InstrumentSearchCriteria.from_json(json)
# print the JSON string representation of the object
print(InstrumentSearchCriteria.to_json())

# convert the object into a dict
instrument_search_criteria_dict = instrument_search_criteria_instance.to_dict()
# create an instance of InstrumentSearchCriteria from a dict
instrument_search_criteria_from_dict = InstrumentSearchCriteria.from_dict(instrument_search_criteria_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


