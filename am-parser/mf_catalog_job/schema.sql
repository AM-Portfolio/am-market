-- MF catalog from NSE MASTER_DOWNLOAD (Step 0)

CREATE TABLE IF NOT EXISTS mf_scheme (
  scheme_code TEXT PRIMARY KEY,
  unique_sr_no TEXT,
  rta_scheme_code TEXT,
  amc_scheme_code TEXT,
  isin TEXT,
  amc_code TEXT,
  scheme_type TEXT,
  plan_type TEXT,
  scheme_name TEXT,
  purchase_allowed TEXT,
  purchase_transaction_mode TEXT,
  new_purchase_min_amount TEXT,
  additional_purchase_min_amount TEXT,
  additional_purchase_max_amount TEXT,
  purchase_amount_multiplier TEXT,
  purchase_cutoff_time TEXT,
  redemption_allowed TEXT,
  redemption_transaction_mode TEXT,
  redemption_min_qty TEXT,
  redemption_qty_multiplier TEXT,
  redemption_max_qty TEXT,
  redemption_min_amount TEXT,
  redemption_max_amount TEXT,
  redemption_amount_multiplier TEXT,
  redemption_cutoff_time TEXT,
  rta_agent_code TEXT,
  amc_active_flag TEXT,
  div_reinvest_flag TEXT,
  sip_allowed TEXT,
  stp_enabled TEXT,
  swp_enabled TEXT,
  switch_allowed TEXT,
  settlement_type TEXT,
  amc_ind TEXT,
  face_value TEXT,
  scheme_start_date TEXT,
  maturity_date TEXT,
  exit_load_flag TEXT,
  exit_load TEXT,
  lock_in_period_flag TEXT,
  lock_in_period TEXT,
  channel_partner_code TEXT,
  reopening_date TEXT,
  open_close_ended TEXT,
  latest_nav REAL,
  latest_nav_date TEXT,
  prev_nav REAL,
  prev_nav_date TEXT,
  change_1d_pct REAL,
  updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mf_scheme_isin ON mf_scheme(isin);
CREATE INDEX IF NOT EXISTS idx_mf_scheme_amc ON mf_scheme(amc_code);
CREATE INDEX IF NOT EXISTS idx_mf_scheme_name ON mf_scheme(scheme_name);

CREATE TABLE IF NOT EXISTS mf_nav_eod (
  scheme_code TEXT NOT NULL,
  nav_date TEXT NOT NULL,
  scheme_name TEXT,
  rta_scheme_code TEXT,
  div_flag TEXT,
  isin TEXT,
  nav REAL NOT NULL,
  rta_agent TEXT,
  PRIMARY KEY (scheme_code, nav_date)
);

CREATE INDEX IF NOT EXISTS idx_mf_nav_isin_date ON mf_nav_eod(isin, nav_date);

CREATE TABLE IF NOT EXISTS mf_sip_rule (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  amc_code TEXT,
  amc_name TEXT,
  scheme_code TEXT,
  scheme_name TEXT,
  sip_transaction_mode TEXT,
  sip_frequency TEXT,
  sip_dates TEXT,
  sip_minimum_gap TEXT,
  sip_maximum_gap TEXT,
  sip_installment_gap TEXT,
  sip_status TEXT,
  sip_min_installment_amount TEXT,
  sip_max_installment_amount TEXT,
  sip_multiplier_amount TEXT,
  sip_min_installment_numbers TEXT,
  sip_max_installment_numbers TEXT,
  scheme_isin TEXT,
  scheme_type TEXT,
  pause_flag TEXT,
  pause_min_installments TEXT,
  pause_max_installments TEXT,
  pause_modification_count TEXT,
  UNIQUE (scheme_code, sip_frequency, sip_dates, sip_transaction_mode)
);

CREATE TABLE IF NOT EXISTS mf_stp_rule (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  amc_code TEXT,
  amc_name TEXT,
  nse_scheme_code TEXT,
  scheme_name TEXT,
  scheme_isin TEXT,
  scheme_type TEXT,
  astp_transaction_mode TEXT,
  astp_in_min_amount TEXT,
  astp_in_max_amount TEXT,
  astp_in_multiplier TEXT,
  astp_out_min_amount TEXT,
  astp_out_max_amount TEXT,
  astp_out_multiplier TEXT,
  astp_min_units TEXT,
  astp_max_units TEXT,
  astp_multiplier_units TEXT,
  astp_min_numbers TEXT,
  astp_max_numbers TEXT,
  astp_reg_in TEXT,
  astp_reg_out TEXT,
  astp_frequency TEXT,
  astp_dates TEXT,
  astp_min_gap TEXT,
  astp_max_gap TEXT,
  astp_installment_gap TEXT,
  astp_status TEXT,
  UNIQUE (nse_scheme_code, astp_frequency, astp_dates, astp_transaction_mode, astp_reg_in, astp_reg_out)
);

CREATE TABLE IF NOT EXISTS mf_swp_rule (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  amc_code TEXT,
  amc_name TEXT,
  nse_scheme_code TEXT,
  scheme_name TEXT,
  scheme_isin TEXT,
  scheme_type TEXT,
  aswp_transaction_mode TEXT,
  aswp_min_amount TEXT,
  aswp_max_amount TEXT,
  aswp_multiplier_amount TEXT,
  aswp_min_units TEXT,
  aswp_max_units TEXT,
  aswp_multiplier_units TEXT,
  aswp_min_numbers TEXT,
  aswp_max_numbers TEXT,
  aswp_frequency TEXT,
  aswp_dates TEXT,
  aswp_min_gap TEXT,
  aswp_max_gap TEXT,
  aswp_installment_gap TEXT,
  aswp_status TEXT,
  UNIQUE (nse_scheme_code, aswp_frequency, aswp_dates, aswp_transaction_mode)
);

CREATE TABLE IF NOT EXISTS mf_settlement (
  settlement_type TEXT,
  settlement_no TEXT,
  trading_date TEXT,
  funds_pay_in_date TEXT,
  sec_pay_in_date TEXT,
  funds_pay_out_date TEXT,
  sec_pay_out_date TEXT,
  PRIMARY KEY (settlement_type, settlement_no, trading_date)
);

CREATE TABLE IF NOT EXISTS mf_ingest_run (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  started_at TEXT NOT NULL,
  finished_at TEXT,
  status TEXT NOT NULL,
  file_type TEXT,
  source_path TEXT,
  rows_read INTEGER,
  rows_upserted INTEGER,
  parse_errors INTEGER,
  max_nav_date TEXT,
  scheme_count INTEGER,
  message TEXT
);

CREATE TABLE IF NOT EXISTS mf_scheme_enrichment (
  isin TEXT PRIMARY KEY,
  aum REAL,
  expense_ratio REAL,
  riskometer TEXT,
  sebi_category TEXT,
  sebi_sub_category TEXT,
  source TEXT,
  updated_at TEXT NOT NULL
);
