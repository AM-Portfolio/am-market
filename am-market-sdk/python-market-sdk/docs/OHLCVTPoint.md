# OHLCVTPoint


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**time** | **datetime** |  | [optional] 
**open** | **float** |  | [optional] 
**high** | **float** |  | [optional] 
**low** | **float** |  | [optional] 
**close** | **float** |  | [optional] 
**volume** | **int** |  | [optional] 

## Example

```python
from am_market_client.models.ohlcvt_point import OHLCVTPoint

# TODO update the JSON string below
json = "{}"
# create an instance of OHLCVTPoint from a JSON string
ohlcvt_point_instance = OHLCVTPoint.from_json(json)
# print the JSON string representation of the object
print(OHLCVTPoint.to_json())

# convert the object into a dict
ohlcvt_point_dict = ohlcvt_point_instance.to_dict()
# create an instance of OHLCVTPoint from a dict
ohlcvt_point_from_dict = OHLCVTPoint.from_dict(ohlcvt_point_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


