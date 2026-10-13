package com.am.marketdata.scraper.ipo;

import com.am.marketdata.common.ipo.IpoIssue;
import com.am.marketdata.common.ipo.IpoIssueStatus;
import com.am.marketdata.common.ipo.IpoLifecycle;
import com.am.marketdata.common.ipo.IpoSubscription;
import com.am.marketdata.common.ipo.IpoSubscriptionCategory;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NseIpoMapper {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter NSE_DATE = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd-MMM-yyyy")
            .toFormatter(Locale.ENGLISH);
    private static final Pattern PRICE_RANGE =
            Pattern.compile("(?i)Rs\\.?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*to\\s*Rs\\.?\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern SINGLE_PRICE = Pattern.compile("(?i)Rs\\.?\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern UPDATE_TIME =
            Pattern.compile("Updated as on\\s+(\\d{2}-[A-Za-z]{3}-\\d{4})\\s+(\\d{2}:\\d{2}:\\d{2})", Pattern.CASE_INSENSITIVE);

    private NseIpoMapper() {}

    static List<IpoIssue> mapPast(JsonNode root) {
        List<IpoIssue> out = new ArrayList<>();
        if (root == null || !root.isArray()) {
            return out;
        }
        for (JsonNode row : root) {
            mapIssue(row, IpoLifecycle.PAST, true).ifPresent(out::add);
        }
        return out;
    }

    static List<IpoIssue> mapCurrent(JsonNode root) {
        List<IpoIssue> out = new ArrayList<>();
        if (root == null || !root.isArray()) {
            return out;
        }
        for (JsonNode row : root) {
            mapIssue(row, IpoLifecycle.CURRENT, true).ifPresent(out::add);
        }
        return out;
    }

    static List<IpoIssue> mapUpcoming(JsonNode root) {
        List<IpoIssue> out = new ArrayList<>();
        if (root == null || !root.isArray()) {
            return out;
        }
        for (JsonNode row : root) {
            IpoLifecycle lifecycle = IpoLifecycle.UPCOMING;
            IpoIssueStatus status = mapStatus(text(row, "status"));
            if (status == IpoIssueStatus.CLOSED) {
                lifecycle = IpoLifecycle.PAST;
            } else if (status == IpoIssueStatus.ACTIVE) {
                lifecycle = IpoLifecycle.CURRENT;
            }
            mapIssue(row, lifecycle, true).ifPresent(out::add);
        }
        return out;
    }

    static Optional<IpoSubscription> mapSubscription(String symbol, String series, JsonNode root) {
        if (root == null || !root.isObject()) {
            return Optional.empty();
        }
        JsonNode data = root.get("data");
        if (data == null || !data.isArray()) {
            return Optional.empty();
        }

        Double overallTimes = null;
        Long overallBid = null;
        Long overallOffered = null;
        List<FlatRow> flats = new ArrayList<>();

        for (JsonNode row : data) {
            String category = text(row, "category");
            String srNo = text(row, "srNo");
            if (category == null || "Category".equalsIgnoreCase(category) || "Sr.No.".equalsIgnoreCase(srNo)) {
                continue;
            }
            Long offered = parseLong(text(row, "noOfSharesOffered"));
            Long bid = parseLong(text(row, "noOfsharesBid"));
            Double times = parseDouble(text(row, "noOfTime"));
            if (srNo == null || srNo.isBlank()) {
                if ("Total".equalsIgnoreCase(category)) {
                    overallTimes = times;
                    overallBid = bid;
                    overallOffered = offered;
                }
                continue;
            }
            flats.add(new FlatRow(category, srNo, offered, bid, times));
        }

        List<IpoSubscriptionCategory> tree = buildTree(flats);
        OffsetDateTime updatedAt = parseUpdateTime(text(root, "updateTime"));
        return Optional.of(new IpoSubscription(
                symbol.toUpperCase(Locale.ROOT),
                series,
                updatedAt,
                overallTimes,
                overallBid,
                overallOffered,
                tree));
    }

    private static Optional<IpoIssue> mapIssue(JsonNode row, IpoLifecycle lifecycle, boolean onNse) {
        String symbol = firstText(row, "symbol");
        LocalDate open = parseDate(firstText(row, "ipoStartDate", "issueStartDate"));
        if (symbol == null || symbol.isBlank() || open == null) {
            return Optional.empty();
        }
        String company = firstText(row, "company", "companyName");
        String series = firstText(row, "series", "securityType");
        String slug = firstText(row, "htmSym");
        LocalDate close = parseDate(firstText(row, "ipoEndDate", "issueEndDate"));
        LocalDate listing = parseDate(firstText(row, "listingDate"));
        LocalDate linkRemoval = parseDate(firstText(row, "linkRemovalDate"));
        String priceLabel = firstText(row, "priceRange", "issuePrice");
        if ("-".equals(priceLabel)) {
            priceLabel = firstText(row, "priceRange");
            if ("-".equals(priceLabel)) {
                priceLabel = null;
            }
        }
        Double[] range = parsePriceRange(priceLabel);
        Double issuePrice = parseDouble(dashToNull(firstText(row, "issuePrice")));
        if (issuePrice == null && range[0] != null && range[0].equals(range[1])) {
            issuePrice = range[0];
        }
        Long sharesOffered = parseLong(firstText(row, "noOfSharesOffered", "issueSize"));
        Long sharesBid = parseLong(firstText(row, "noOfsharesBid"));
        Double times = parseDouble(firstText(row, "noOfTime"));
        boolean onBse = "1".equals(firstText(row, "isBse"));
        IpoIssueStatus status = mapStatus(firstText(row, "status"));
        if (lifecycle == IpoLifecycle.PAST && status == IpoIssueStatus.UNKNOWN) {
            status = IpoIssueStatus.CLOSED;
        }

        return Optional.of(new IpoIssue(
                symbol,
                slug,
                company,
                series,
                lifecycle,
                status,
                open,
                close,
                listing,
                linkRemoval,
                "INR",
                range[0],
                range[1],
                issuePrice,
                priceLabel,
                sharesOffered,
                firstText(row, "issueSize"),
                onNse,
                onBse,
                times,
                sharesBid,
                sharesOffered));
    }

    private static List<IpoSubscriptionCategory> buildTree(List<FlatRow> flats) {
        flats.sort(Comparator.comparing(f -> f.srNo, NseIpoMapper::compareSrNo));
        Map<String, MutableNode> nodes = new HashMap<>();
        List<MutableNode> roots = new ArrayList<>();
        for (FlatRow flat : flats) {
            MutableNode node = new MutableNode(flat);
            nodes.put(flat.srNo, node);
            String parent = parentSrNo(flat.srNo);
            if (parent == null || !nodes.containsKey(parent)) {
                roots.add(node);
            } else {
                nodes.get(parent).children.add(node);
            }
        }
        List<IpoSubscriptionCategory> out = new ArrayList<>();
        for (MutableNode root : roots) {
            out.add(root.toCategory());
        }
        return out;
    }

    private static String parentSrNo(String srNo) {
        if (srNo == null || srNo.isBlank()) {
            return null;
        }
        int paren = srNo.indexOf('(');
        if (paren > 0) {
            return srNo.substring(0, paren);
        }
        int dot = srNo.lastIndexOf('.');
        if (dot > 0) {
            return srNo.substring(0, dot);
        }
        return null;
    }

    private static int compareSrNo(String a, String b) {
        return normalizeSrNo(a).compareTo(normalizeSrNo(b));
    }

    private static String normalizeSrNo(String srNo) {
        return srNo == null ? "" : srNo.replace("(", ".").replace(")", "");
    }

    private static String categoryCode(String name, String srNo) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (n.contains("qualified institutional")) {
            return "QIB";
        }
        if (n.contains("foreign institutional")) {
            return "FII";
        }
        if (n.contains("domestic financial")) {
            return "DFI";
        }
        if (n.contains("mutual fund")) {
            return "MF";
        }
        if (n.contains("non institutional") && n.contains("ten lakh") && n.contains("more than")) {
            return "NII_ABOVE_10L";
        }
        if (n.contains("non institutional") && n.contains("two lakh")) {
            return "NII_2L_10L";
        }
        if (n.contains("non institutional")) {
            return "NII";
        }
        if (n.contains("retail")) {
            return "RII";
        }
        if (n.contains("employee")) {
            return "EMPLOYEE";
        }
        if (n.contains("cut off")) {
            return "CUT_OFF";
        }
        if (n.contains("price bid")) {
            return "PRICE_BID";
        }
        if (n.contains("corporate")) {
            return "CORPORATE";
        }
        if (n.equals("others") || n.contains("others")) {
            return "OTHERS";
        }
        if (n.contains("individual")) {
            return "INDIVIDUAL";
        }
        String base = srNo == null ? "CAT" : "SR_" + srNo.replace("(", "_").replace(")", "").replace(".", "_");
        return base.toUpperCase(Locale.ROOT);
    }

    static IpoIssueStatus mapStatus(String status) {
        if (status == null || status.isBlank()) {
            return IpoIssueStatus.UNKNOWN;
        }
        String s = status.trim().toLowerCase(Locale.ROOT);
        if (s.contains("active")) {
            return IpoIssueStatus.ACTIVE;
        }
        if (s.contains("forthcoming") || s.contains("upcoming")) {
            return IpoIssueStatus.FORTHCOMING;
        }
        if (s.contains("closed")) {
            return IpoIssueStatus.CLOSED;
        }
        return IpoIssueStatus.UNKNOWN;
    }

    static LocalDate parseDate(String raw) {
        String v = dashToNull(raw);
        if (v == null) {
            return null;
        }
        try {
            return LocalDate.parse(v, NSE_DATE);
        } catch (Exception ignored) {
            return null;
        }
    }

    static Double[] parsePriceRange(String label) {
        Double[] out = new Double[] {null, null};
        if (label == null || label.isBlank() || "-".equals(label.trim())) {
            return out;
        }
        Matcher range = PRICE_RANGE.matcher(label);
        if (range.find()) {
            out[0] = Double.parseDouble(range.group(1));
            out[1] = Double.parseDouble(range.group(2));
            return out;
        }
        Matcher single = SINGLE_PRICE.matcher(label);
        if (single.find()) {
            double p = Double.parseDouble(single.group(1));
            out[0] = p;
            out[1] = p;
        }
        return out;
    }

    static Long parseLong(String raw) {
        String v = dashToNull(raw);
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Math.round(Double.parseDouble(v.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Double parseDouble(String raw) {
        String v = dashToNull(raw);
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static OffsetDateTime parseUpdateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher m = UPDATE_TIME.matcher(raw);
        if (!m.find()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(m.group(1), NSE_DATE);
            LocalDateTime ldt = LocalDateTime.of(date, java.time.LocalTime.parse(m.group(2)));
            return ldt.atZone(IST).toOffsetDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private static String dashToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty() || "-".equals(t)) {
            return null;
        }
        return t;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText(null);
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String v = text(node, field);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static final class FlatRow {
        private final String category;
        private final String srNo;
        private final Long offered;
        private final Long bid;
        private final Double times;

        private FlatRow(String category, String srNo, Long offered, Long bid, Double times) {
            this.category = category;
            this.srNo = srNo;
            this.offered = offered;
            this.bid = bid;
            this.times = times;
        }
    }

    private static final class MutableNode {
        private final FlatRow row;
        private final List<MutableNode> children = new ArrayList<>();

        private MutableNode(FlatRow row) {
            this.row = row;
        }

        private IpoSubscriptionCategory toCategory() {
            List<IpoSubscriptionCategory> kids = new ArrayList<>();
            for (MutableNode child : children) {
                kids.add(child.toCategory());
            }
            return new IpoSubscriptionCategory(
                    categoryCode(row.category, row.srNo),
                    row.category,
                    row.srNo,
                    row.offered,
                    row.bid,
                    row.times,
                    kids);
        }
    }
}
