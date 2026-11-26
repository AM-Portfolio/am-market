-- Previous EOD NAV for day-over-day comparison (denorm on mf_scheme).
-- latest_* = newest mf_nav_eod point; prev_* = second-newest (null if only one point).
-- Never invent; change_1d_pct = ((latest - prev) / prev) * 100 when both present.

ALTER TABLE mf_scheme ADD COLUMN IF NOT EXISTS prev_nav DOUBLE PRECISION;
ALTER TABLE mf_scheme ADD COLUMN IF NOT EXISTS prev_nav_date VARCHAR(16);
ALTER TABLE mf_scheme ADD COLUMN IF NOT EXISTS change_1d_pct DOUBLE PRECISION;
