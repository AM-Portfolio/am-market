-- Denormalized period returns for Explore search (computed from mf_nav_eod; never invent).
-- Populated by catalog/compute_scheme_returns.py after NAV ingest / historical backfill.

CREATE TABLE IF NOT EXISTS mf_scheme_returns (
  isin VARCHAR(16) PRIMARY KEY,
  scheme_code VARCHAR(64),
  ret_1m DOUBLE PRECISION,
  ret_3m DOUBLE PRECISION,
  ret_1y DOUBLE PRECISION,
  ret_3y DOUBLE PRECISION,
  ret_5y DOUBLE PRECISION,
  as_of VARCHAR(16),
  point_count INTEGER,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mf_scheme_returns_scheme ON mf_scheme_returns(scheme_code);
