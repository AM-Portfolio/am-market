# Audit


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**created_at** | **datetime** |  | [optional] 
**version** | **int** |  | [optional] 

## Example

```python
from am_market_client.models.audit import Audit

# TODO update the JSON string below
json = "{}"
# create an instance of Audit from a JSON string
audit_instance = Audit.from_json(json)
# print the JSON string representation of the object
print(Audit.to_json())

# convert the object into a dict
audit_dict = audit_instance.to_dict()
# create an instance of Audit from a dict
audit_from_dict = Audit.from_dict(audit_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


