package com.am.marketdata.upstock.model;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderData {
    @JsonProperty("order_id")
    private String orderId;
    private String status;
    private String symbol;
    private String side;
    @JsonProperty("order_type")
    private String orderType;
    private Double quantity;
    private Double price;
    @JsonProperty("trigger_price")
    private Double triggerPrice;
    @JsonProperty("disclosed_quantity")
    private Double disclosedQuantity;
    private String validity;
    @JsonProperty("placed_by")
    private String placedBy;
    @JsonProperty("exchange_order_id")
    private String exchangeOrderId;
    @JsonProperty("parent_order_id")
    private String parentOrderId;
    @JsonProperty("is_amo")
    private Boolean isAmo;
    @JsonProperty("average_price")
    private Double averagePrice;
    @JsonProperty("filled_quantity")
    private Double filledQuantity;
    @JsonProperty("pending_quantity")
    private Double pendingQuantity;
    @JsonProperty("exchange_timestamp")
    private String exchangeTimestamp;
    @JsonProperty("order_timestamp")
    private String orderTimestamp;
} 