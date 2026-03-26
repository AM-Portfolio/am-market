# IndexMetadata


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**index_name** | **str** |  | [optional] 
**open** | **float** |  | [optional] 
**high** | **float** |  | [optional] 
**low** | **float** |  | [optional] 
**previous_close** | **float** |  | [optional] 
**last** | **float** |  | [optional] 
**perc_change** | **float** |  | [optional] 
**change** | **float** |  | [optional] 
**time_val** | **str** |  | [optional] 
**year_high** | **float** |  | [optional] 
**year_low** | **float** |  | [optional] 
**indicative_close** | **float** |  | [optional] 
**total_traded_volume** | **int** |  | [optional] 
**total_traded_value** | **float** |  | [optional] 
**ffmc_sum** | **float** |  | [optional] 

## Example

```python
from am_market_client.models.index_metadata import IndexMetadata

# TODO update the JSON string below
json = "{}"
# create an instance of IndexMetadata from a JSON string
index_metadata_instance = IndexMetadata.from_json(json)
# print the JSON string representation of the object
print(IndexMetadata.to_json())

# convert the object into a dict
index_metadata_dict = index_metadata_instance.to_dict()
# create an instance of IndexMetadata from a dict
index_metadata_from_dict = IndexMetadata.from_dict(index_metadata_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


