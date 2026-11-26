-- Phase 3: vendor analytics columns on mf_scheme_enrichment (benchmark / ratings / holdings).
-- Populated only after licensed vendor or AMC factsheet contract; never invent client-side.

ALTER TABLE mf_scheme_enrichment ADD COLUMN IF NOT EXISTS benchmark_name VARCHAR(256);
ALTER TABLE mf_scheme_enrichment ADD COLUMN IF NOT EXISTS benchmark_code VARCHAR(64);
ALTER TABLE mf_scheme_enrichment ADD COLUMN IF NOT EXISTS rating_crisil VARCHAR(32);
ALTER TABLE mf_scheme_enrichment ADD COLUMN IF NOT EXISTS rating_morningstar VARCHAR(32);
ALTER TABLE mf_scheme_enrichment ADD COLUMN IF NOT EXISTS rating_value_research VARCHAR(32);
ALTER TABLE mf_scheme_enrichment ADD COLUMN IF NOT EXISTS holdings_json TEXT;
