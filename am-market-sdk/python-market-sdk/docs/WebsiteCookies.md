# WebsiteCookies


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**website_url** | **str** |  | [optional] 
**website_name** | **str** |  | [optional] 
**cookies** | [**List[CookieInfo]**](CookieInfo.md) |  | [optional] 
**cookies_string** | **str** |  | [optional] 

## Example

```python
from am_market_client.models.website_cookies import WebsiteCookies

# TODO update the JSON string below
json = "{}"
# create an instance of WebsiteCookies from a JSON string
website_cookies_instance = WebsiteCookies.from_json(json)
# print the JSON string representation of the object
print(WebsiteCookies.to_json())

# convert the object into a dict
website_cookies_dict = website_cookies_instance.to_dict()
# create an instance of WebsiteCookies from a dict
website_cookies_from_dict = WebsiteCookies.from_dict(website_cookies_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


