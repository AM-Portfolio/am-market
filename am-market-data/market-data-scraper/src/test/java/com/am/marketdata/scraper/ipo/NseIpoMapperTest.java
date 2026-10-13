package com.am.marketdata.scraper.ipo;

import com.am.marketdata.common.ipo.IpoIssue;
import com.am.marketdata.common.ipo.IpoIssueStatus;
import com.am.marketdata.common.ipo.IpoLifecycle;
import com.am.marketdata.common.ipo.IpoSubscription;
import com.am.marketdata.common.ipo.IpoSubscriptionCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NseIpoMapperTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapCurrentParsesPriceRangeAndDates() throws Exception {
        JsonNode root = mapper.readTree("""
                [{
                  "symbol": "JNPR",
                  "companyName": "Juniper Hotels Limited",
                  "series": "EQ",
                  "htmSym": "juniper-hotels",
                  "issueStartDate": "30-Jul-2026",
                  "issueEndDate": "01-Aug-2026",
                  "listingDate": "08-Aug-2026",
                  "priceRange": "Rs.100 to Rs.110",
                  "issueSize": "25000000",
                  "noOfsharesBid": "50000000",
                  "noOfTime": "2.0",
                  "isBse": "1",
                  "status": "Active"
                }]
                """);

        List<IpoIssue> issues = NseIpoMapper.mapCurrent(root);
        assertEquals(1, issues.size());
        IpoIssue issue = issues.get(0);
        assertEquals("JNPR", issue.getSymbol());
        assertEquals(IpoLifecycle.CURRENT, issue.getLifecycle());
        assertEquals(IpoIssueStatus.ACTIVE, issue.getStatus());
        assertEquals(LocalDate.of(2026, 7, 30), issue.getOpenDate());
        assertEquals(LocalDate.of(2026, 8, 1), issue.getCloseDate());
        assertEquals(100.0, issue.getPriceMin());
        assertEquals(110.0, issue.getPriceMax());
        assertTrue(issue.isOnNse());
        assertTrue(issue.isOnBse());
        assertEquals(2.0, issue.getSubscriptionTimes());
        assertEquals("JNPR:2026-07-30", issue.id());
    }

    @Test
    void mapPastSkipsRowsWithoutSymbolOrOpenDate() throws Exception {
        JsonNode root = mapper.readTree("""
                [
                  {"company": "No Symbol", "ipoStartDate": "01-Jan-2024"},
                  {"symbol": "ABC", "company": "No Date"},
                  {
                    "symbol": "XYZ",
                    "company": "Good Co",
                    "ipoStartDate": "15-Jan-2024",
                    "ipoEndDate": "17-Jan-2024",
                    "issuePrice": "250",
                    "series": "EQ"
                  }
                ]
                """);

        List<IpoIssue> issues = NseIpoMapper.mapPast(root);
        assertEquals(1, issues.size());
        assertEquals("XYZ", issues.get(0).getSymbol());
        assertEquals(IpoLifecycle.PAST, issues.get(0).getLifecycle());
        assertEquals(IpoIssueStatus.CLOSED, issues.get(0).getStatus());
        assertEquals(250.0, issues.get(0).getIssuePrice());
    }

    @Test
    void mapUpcomingPromotesActiveAndClosed() throws Exception {
        JsonNode root = mapper.readTree("""
                [
                  {
                    "symbol": "UP1",
                    "companyName": "Soon",
                    "issueStartDate": "10-Sep-2026",
                    "status": "Forthcoming"
                  },
                  {
                    "symbol": "CUR1",
                    "companyName": "Live",
                    "issueStartDate": "01-Aug-2026",
                    "status": "Active"
                  },
                  {
                    "symbol": "PAST1",
                    "companyName": "Done",
                    "issueStartDate": "01-Jul-2026",
                    "status": "Closed"
                  }
                ]
                """);

        List<IpoIssue> issues = NseIpoMapper.mapUpcoming(root);
        assertEquals(3, issues.size());
        assertEquals(IpoLifecycle.UPCOMING, issues.get(0).getLifecycle());
        assertEquals(IpoLifecycle.CURRENT, issues.get(1).getLifecycle());
        assertEquals(IpoLifecycle.PAST, issues.get(2).getLifecycle());
    }

    @Test
    void mapSubscriptionBuildsTreeAndOverallTotal() throws Exception {
        JsonNode root = mapper.readTree("""
                {
                  "updateTime": "Updated as on 04-Aug-2026 15:30:00",
                  "data": [
                    {"srNo": "Sr.No.", "category": "Category", "noOfSharesOffered": "-", "noOfsharesBid": "-", "noOfTime": "-"},
                    {"srNo": "1", "category": "Qualified Institutional Buyers (QIBs)", "noOfSharesOffered": "1000", "noOfsharesBid": "5000", "noOfTime": "5"},
                    {"srNo": "1.1", "category": "Foreign Institutional Investors", "noOfSharesOffered": "400", "noOfsharesBid": "2000", "noOfTime": "5"},
                    {"srNo": "2", "category": "Retail Individual Investors (RIIs)", "noOfSharesOffered": "500", "noOfsharesBid": "1500", "noOfTime": "3"},
                    {"srNo": "", "category": "Total", "noOfSharesOffered": "1500", "noOfsharesBid": "6500", "noOfTime": "4.33"}
                  ]
                }
                """);

        Optional<IpoSubscription> sub = NseIpoMapper.mapSubscription("JNPR", "EQ", root);
        assertTrue(sub.isPresent());
        IpoSubscription s = sub.get();
        assertEquals(4.33, s.getOverallTimes());
        assertEquals(6500L, s.getOverallSharesBid());
        assertEquals(1500L, s.getOverallSharesOffered());
        assertEquals(2, s.getCategories().size());

        IpoSubscriptionCategory qib = s.getCategories().get(0);
        assertEquals("QIB", qib.getCode());
        assertEquals(1, qib.getChildren().size());
        assertEquals("FII", qib.getChildren().get(0).getCode());
        assertEquals("RII", s.getCategories().get(1).getCode());
        assertFalse(s.getUpdatedAt() == null);
    }

    @Test
    void parsePriceRangeHandlesSingleAndDash() {
        Double[] range = NseIpoMapper.parsePriceRange("Rs.99");
        assertEquals(99.0, range[0]);
        assertEquals(99.0, range[1]);
        Double[] empty = NseIpoMapper.parsePriceRange("-");
        assertEquals(null, empty[0]);
    }
}
