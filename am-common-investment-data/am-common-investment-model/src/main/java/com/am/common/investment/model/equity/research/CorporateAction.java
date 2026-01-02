package com.am.common.investment.model.equity.research;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.am.common.investment.model.equity.financial.BaseModel;

import lombok.Data;

@Data
@Document(collection = "corporate_actions")
public class CorporateAction extends BaseModel {
    private List<Dividend> dividends;
    private List<Bonus> bonuses;
    private List<RightIssue> rightIssues;
    private List<BoardMeeting> boardMeetings;
    private List<GeneralMeeting> generalMeetings;
    private List<BulkDeals> bulkDeals;
    private List<BlockDeals> blockDeals;
}