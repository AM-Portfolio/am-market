# IngestionJobLog


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **str** |  | [optional] 
**job_id** | **str** |  | [optional] 
**start_time** | **datetime** |  | [optional] 
**end_time** | **datetime** |  | [optional] 
**status** | **str** |  | [optional] 
**total_symbols** | **int** |  | [optional] 
**success_count** | **int** |  | [optional] 
**failure_count** | **int** |  | [optional] 
**failed_symbols** | **List[str]** |  | [optional] 
**duration_ms** | **int** |  | [optional] 
**payload_size** | **int** |  | [optional] 
**message** | **str** |  | [optional] 
**logs** | **List[str]** |  | [optional] 

## Example

```python
from am_market_client.models.ingestion_job_log import IngestionJobLog

# TODO update the JSON string below
json = "{}"
# create an instance of IngestionJobLog from a JSON string
ingestion_job_log_instance = IngestionJobLog.from_json(json)
# print the JSON string representation of the object
print(IngestionJobLog.to_json())

# convert the object into a dict
ingestion_job_log_dict = ingestion_job_log_instance.to_dict()
# create an instance of IngestionJobLog from a dict
ingestion_job_log_from_dict = IngestionJobLog.from_dict(ingestion_job_log_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


