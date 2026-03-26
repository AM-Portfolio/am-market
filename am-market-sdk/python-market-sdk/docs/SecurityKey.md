# SecurityKey


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**symbol** | **str** |  | [optional] 
**isin** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.security_key import SecurityKey

# TODO update the JSON string below
json = "{}"
# create an instance of SecurityKey from a JSON string
security_key_instance = SecurityKey.from_json(json)
# print the JSON string representation of the object
print(SecurityKey.to_json())

# convert the object into a dict
security_key_dict = security_key_instance.to_dict()
# create an instance of SecurityKey from a dict
security_key_from_dict = SecurityKey.from_dict(security_key_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


