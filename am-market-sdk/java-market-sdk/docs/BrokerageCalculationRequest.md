

# BrokerageCalculationRequest


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**tradingSymbol** | **String** |  |  [optional] |
|**quantity** | **Integer** |  |  [optional] |
|**buyPrice** | **BigDecimal** |  |  [optional] |
|**sellPrice** | **BigDecimal** |  |  [optional] |
|**exchange** | **String** |  |  [optional] |
|**tradeType** | [**TradeTypeEnum**](#TradeTypeEnum) |  |  [optional] |
|**brokerType** | [**BrokerTypeEnum**](#BrokerTypeEnum) |  |  [optional] |
|**brokerName** | **String** |  |  [optional] |
|**brokerFlatFee** | **BigDecimal** |  |  [optional] |
|**brokerPercentageFee** | **BigDecimal** |  |  [optional] |
|**stateCode** | **String** |  |  [optional] |



## Enum: TradeTypeEnum

| Name | Value |
|---- | -----|
| DELIVERY | &quot;DELIVERY&quot; |
| INTRADAY | &quot;INTRADAY&quot; |



## Enum: BrokerTypeEnum

| Name | Value |
|---- | -----|
| DISCOUNT | &quot;DISCOUNT&quot; |
| FULL_SERVICE | &quot;FULL_SERVICE&quot; |



