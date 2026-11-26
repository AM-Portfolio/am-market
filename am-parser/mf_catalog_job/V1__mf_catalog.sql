-- MF catalog (Postgres) — promoted from am-mutual-fund/catalog/schema.sql
-- Applied when am.market.mf.jdbc-url points at Postgres (Flyway or manual).

CREATE TABLE IF NOT EXISTS mf_scheme (
  scheme_code VARCHAR(64) PRIMARY KEY,
  unique_sr_no VARCHAR(64),
  rta_scheme_code VARCHAR(64),
  amc_scheme_code VARCHAR(64),
  isin VARCHAR(16),
  amc_code VARCHAR(64),
  scheme_type VARCHAR(64),
  plan_type VARCHAR(64),
  scheme_name TEXT,
  purchase_allowed VARCHAR(8),
  purchase_transaction_mode VARCHAR(32),
  new_purchase_min_amount VARCHAR(32),
  additional_purchase_min_amount VARCHAR(32),
  additional_purchase_max_amount VARCHAR(32),
  purchase_amount_multiplier VARCHAR(32),
  purchase_cutoff_time VARCHAR(32),
  redemption_allowed VARCHAR(8),
  redemption_transaction_mode VARCHAR(32),
  redemption_min_qty VARCHAR(32),
  redemption_qty_multiplier VARCHAR(32),
  redemption_max_qty VARCHAR(32),
  redemption_min_amount VARCHAR(32),
  redemption_max_amount VARCHAR(32),
  redemption_amount_multiplier VARCHAR(32),
  redemption_cutoff_time VARCHAR(32),
  rta_agent_code VARCHAR(32),
  amc_active_flag VARCHAR(8),
  div_reinvest_flag VARCHAR(8),
  sip_allowed VARCHAR(8),
  stp_enabled VARCHAR(8),
  swp_enabled VARCHAR(8),
  switch_allowed VARCHAR(8),
  settlement_type VARCHAR(32),
  amc_ind VARCHAR(32),
  face_value VARCHAR(32),
  scheme_start_date VARCHAR(32),
  maturity_date VARCHAR(32),
  exit_load_flag VARCHAR(8),
  exit_load TEXT,
  lock_in_period_flag VARCHAR(8),
  lock_in_period VARCHAR(32),
  channel_partner_code VARCHAR(64),
  reopening_date VARCHAR(32),
  open_close_ended VARCHAR(32),
  latest_nav DOUBLE PRECISION,
  latest_nav_date VARCHAR(16),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mf_scheme_isin ON mf_scheme(isin);
CREATE INDEX IF NOT EXISTS idx_mf_scheme_amc ON mf_scheme(amc_code);
CREATE INDEX IF NOT EXISTS idx_mf_scheme_name ON mf_scheme(scheme_name);

CREATE TABLE IF NOT EXISTS mf_nav_eod (
  scheme_code VARCHAR(64) NOT NULL,
  nav_date VARCHAR(16) NOT NULL,
  scheme_name TEXT,
  rta_scheme_code VARCHAR(64),
  div_flag VARCHAR(8),
  isin VARCHAR(16),
  nav DOUBLE PRECISION NOT NULL,
  rta_agent VARCHAR(32),
  PRIMARY KEY (scheme_code, nav_date)
);

CREATE INDEX IF NOT EXISTS idx_mf_nav_isin_date ON mf_nav_eod(isin, nav_date);

CREATE TABLE IF NOT EXISTS mf_sip_rule (
  id BIGSERIAL PRIMARY KEY,
  amc_code VARCHAR(64),
  amc_name TEXT,
  scheme_code VARCHAR(64),
  scheme_name TEXT,
  sip_transaction_mode VARCHAR(32),
  sip_frequency VARCHAR(32),
  sip_dates TEXT,
  sip_minimum_gap VARCHAR(32),
  sip_maximum_gap VARCHAR(32),
  sip_installment_gap VARCHAR(32),
  sip_status VARCHAR(32),
  sip_min_installment_amount VARCHAR(32),
  sip_max_installment_amount VARCHAR(32),
  sip_multiplier_amount VARCHAR(32),
  sip_min_installment_numbers VARCHAR(32),
  sip_max_installment_numbers VARCHAR(32),
  scheme_isin VARCHAR(16),
  scheme_type VARCHAR(64),
  pause_flag VARCHAR(8),
  pause_min_installments VARCHAR(32),
  pause_max_installments VARCHAR(32),
  pause_modification_count VARCHAR(32),
  UNIQUE (scheme_code, sip_frequency, sip_dates, sip_transaction_mode)
);

CREATE TABLE IF NOT EXISTS mf_stp_rule (
  id BIGSERIAL PRIMARY KEY,
  amc_code VARCHAR(64),
  amc_name TEXT,
  nse_scheme_code VARCHAR(64),
  scheme_name TEXT,
  scheme_isin VARCHAR(16),
  scheme_type VARCHAR(64),
  astp_transaction_mode VARCHAR(32),
  astp_in_min_amount VARCHAR(32),
  astp_in_max_amount VARCHAR(32),
  astp_in_multiplier VARCHAR(32),
  astp_out_min_amount VARCHAR(32),
  astp_out_max_amount VARCHAR(32),
  astp_out_multiplier VARCHAR(32),
  astp_min_units VARCHAR(32),
  astp_max_units VARCHAR(32),
  astp_multiplier_units VARCHAR(32),
  astp_min_numbers VARCHAR(32),
  astp_max_numbers VARCHAR(32),
  astp_reg_in VARCHAR(32),
  astp_reg_out VARCHAR(32),
  astp_frequency VARCHAR(32),
  astp_dates TEXT,
  astp_min_gap VARCHAR(32),
  astp_max_gap VARCHAR(32),
  astp_installment_gap VARCHAR(32),
  astp_status VARCHAR(32),
  UNIQUE (nse_scheme_code, astp_frequency, astp_dates, astp_transaction_mode, astp_reg_in, astp_reg_out)
);

CREATE TABLE IF NOT EXISTS mf_swp_rule (
  id BIGSERIAL PRIMARY KEY,
  amc_code VARCHAR(64),
  amc_name TEXT,
  nse_scheme_code VARCHAR(64),
  scheme_name TEXT,
  scheme_isin VARCHAR(16),
  scheme_type VARCHAR(64),
  aswp_transaction_mode VARCHAR(32),
  aswp_min_amount VARCHAR(32),
  aswp_max_amount VARCHAR(32),
  aswp_multiplier_amount VARCHAR(32),
  aswp_min_units VARCHAR(32),
  aswp_max_units VARCHAR(32),
  aswp_multiplier_units VARCHAR(32),
  aswp_min_numbers VARCHAR(32),
  aswp_max_numbers VARCHAR(32),
  aswp_frequency VARCHAR(32),
  aswp_dates TEXT,
  aswp_min_gap VARCHAR(32),
  aswp_max_gap VARCHAR(32),
  aswp_installment_gap VARCHAR(32),
  aswp_status VARCHAR(32),
  UNIQUE (nse_scheme_code, aswp_frequency, aswp_dates, aswp_transaction_mode)
);

CREATE TABLE IF NOT EXISTS mf_settlement (
  settlement_type VARCHAR(32),
  settlement_no VARCHAR(64),
  trading_date VARCHAR(16),
  funds_pay_in_date VARCHAR(16),
  sec_pay_in_date VARCHAR(16),
  funds_pay_out_date VARCHAR(16),
  sec_pay_out_date VARCHAR(16),
  PRIMARY KEY (settlement_type, settlement_no, trading_date)
);

CREATE TABLE IF NOT EXISTS mf_ingest_run (
  id BIGSERIAL PRIMARY KEY,
  started_at TIMESTAMPTZ NOT NULL,
  finished_at TIMESTAMPTZ,
  status VARCHAR(32) NOT NULL,
  file_type VARCHAR(16),
  source_path TEXT,
  rows_read INTEGER,
  rows_upserted INTEGER,
  parse_errors INTEGER,
  max_nav_date VARCHAR(16),
  scheme_count INTEGER,
  message TEXT
);

CREATE TABLE IF NOT EXISTS mf_scheme_enrichment (
  isin VARCHAR(16) PRIMARY KEY,
  aum DOUBLE PRECISION,
  expense_ratio DOUBLE PRECISION,
  riskometer VARCHAR(64),
  sebi_category VARCHAR(128),
  sebi_sub_category VARCHAR(128),
  benchmark_name VARCHAR(256),
  benchmark_code VARCHAR(64),
  rating_crisil VARCHAR(32),
  rating_morningstar VARCHAR(32),
  rating_value_research VARCHAR(32),
  holdings_json TEXT,
  source VARCHAR(64),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
