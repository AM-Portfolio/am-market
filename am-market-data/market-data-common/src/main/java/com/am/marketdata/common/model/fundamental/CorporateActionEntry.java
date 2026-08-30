package com.am.marketdata.common.model.fundamental;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Historical corporate action event (Dividends, Bonus, Stock Splits, Rights issues).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorporateActionEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Type of corporate action: "DIVIDEND", "BONUS", "SPLIT", "RIGHTS", etc.
     */
    private String type;

    /**
     * Human-readable description or details of the event.
     */
    private String description;

    /**
     * Date of formal announcement.
     */
    private String announcementDate;

    /**
     * Ex-Date for eligibility.
     */
    private String exDate;

    /**
     * Record Date for eligibility.
     */
    private String recordDate;

    /**
     * Dividend amount per share (if applicable).
     */
    private Double amount;

    /**
     * Bonus/Split/Rights ratio string (e.g., "1:1", "1:5", "2:1").
     */
    private String ratio;
}
