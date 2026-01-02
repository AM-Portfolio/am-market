package com.am.common.investment.model.equity.research;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

import com.am.common.investment.model.equity.financial.BaseModel;

import lombok.Data;

@Data
@Document(collection = "stock_sentinal")
public class StockSentinal extends BaseModel {
    /** 
     * Date of the sentiment analysis in YYYYMMDD format
     * Example: 20250406 for April 6, 2025
     */
    private String periodDate;
    
    /** Percentage of users who chose to buy */
    private Double buyRatio;
    
    /** Percentage of users who chose to sell */
    private Double sellRatio;
    
    /** Percentage of users who chose to hold */
    private Double holdRatio;
    
    /** User's choice (buy, sell, hold) */
    private String userChoice;
    
    /** Sentiment score */
    private Double sentimentScore;
    
    /** Sentiment type (positive, negative, neutral) */
    private String sentimentType;
    
    /** Daily update flag */
    private Boolean dailyUpdate;
}
