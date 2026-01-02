package com.am.common.investment.model.equity.research;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.am.common.investment.model.equity.financial.BaseModel;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "company_insights")
public class CompanyInsights extends BaseModel {
    private List<CompanyInsightsList> pros;
    private List<CompanyInsightsList> cons;


/**
 * Represents a list of company insights with analysis details
 */
@Data
class CompanyInsightsList {
    private String[] insights;
    private CompanyInsightAnalysis[] analysis;
}

/**
 * Represents detailed analysis of a company insight
 */
@Data
class CompanyInsightAnalysis {
    private String metric;
    private Double currentValue;
    private Double comparisonValue;
    private String timePeriod;
    private String unit;
    private String analysisType;
}
}
