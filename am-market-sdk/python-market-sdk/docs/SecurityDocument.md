# SecurityDocument


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **str** |  | [optional] 
**key** | [**SecurityKey**](SecurityKey.md) |  | [optional] 
**metadata** | [**SecurityMetadata**](SecurityMetadata.md) |  | [optional] 
**audit** | [**Audit**](Audit.md) |  | [optional] 

## Example

```python
from am_market_client.models.security_document import SecurityDocument

# TODO update the JSON string below
json = "{}"
# create an instance of SecurityDocument from a JSON string
security_document_instance = SecurityDocument.from_json(json)
# print the JSON string representation of the object
print(SecurityDocument.to_json())

# convert the object into a dict
security_document_dict = security_document_instance.to_dict()
# create an instance of SecurityDocument from a dict
security_document_from_dict = SecurityDocument.from_dict(security_document_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


