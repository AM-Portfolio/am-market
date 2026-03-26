

# HistoricalDataRequest


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**symbols** | **String** |  |  [optional] |
|**from** | **String** | Start date in yyyy-MM-dd format |  [optional] |
|**to** | **String** | End date in yyyy-MM-dd format (optional, defaults to current date) |  [optional] |
|**interval** | [**IntervalEnum**](#IntervalEnum) |  |  [optional] |
|**continuous** | **Boolean** |  |  [optional] |
|**instrumentType** | **String** |  |  [optional] |
|**forceRefresh** | **Boolean** |  |  [optional] |
|**filterType** | **String** |  |  [optional] |
|**filterFrequency** | **Integer** |  |  [optional] |
|**additionalParams** | **Map&lt;String, Object&gt;** |  |  [optional] |
|**isIndexSymbol** | **Boolean** | Whether the symbols represent indices that should be expanded to constituent stocks |  [optional] |



## Enum: IntervalEnum

| Name | Value |
|---- | -----|
| MINUTE | &quot;MINUTE&quot; |
| THREE_MINUTE | &quot;THREE_MINUTE&quot; |
| FIVE_MINUTE | &quot;FIVE_MINUTE&quot; |
| TEN_MINUTE | &quot;TEN_MINUTE&quot; |
| FIFTEEN_MINUTE | &quot;FIFTEEN_MINUTE&quot; |
| THIRTY_MINUTE | &quot;THIRTY_MINUTE&quot; |
| HOUR | &quot;HOUR&quot; |
| FOUR_HOUR | &quot;FOUR_HOUR&quot; |
| DAY | &quot;DAY&quot; |
| WEEK | &quot;WEEK&quot; |
| MONTH | &quot;MONTH&quot; |
| YEAR | &quot;YEAR&quot; |



