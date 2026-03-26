# CookieInfo


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **str** |  | [optional] 
**value** | **str** |  | [optional] 
**domain** | **str** |  | [optional] 
**path** | **str** |  | [optional] 
**secure** | **bool** |  | [optional] 
**http_only** | **bool** |  | [optional] 
**same_site** | **str** |  | [optional] 
**expiry** | **int** |  | [optional] 

## Example

```python
from am_market_client.models.cookie_info import CookieInfo

# TODO update the JSON string below
json = "{}"
# create an instance of CookieInfo from a JSON string
cookie_info_instance = CookieInfo.from_json(json)
# print the JSON string representation of the object
print(CookieInfo.to_json())

# convert the object into a dict
cookie_info_dict = cookie_info_instance.to_dict()
# create an instance of CookieInfo from a dict
cookie_info_from_dict = CookieInfo.from_dict(cookie_info_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


