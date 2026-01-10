-- Watchlist Database Migration
-- Version: 1.0
-- Description: Create watchlist_items table for user watchlist management

CREATE TABLE IF NOT EXISTS watchlist_items (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    symbol VARCHAR(50) NOT NULL,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, symbol)
);

CREATE INDEX IF NOT EXISTS idx_watchlist_user_id ON watchlist_items(user_id);

-- Sample data for testing (optional)
-- INSERT INTO watchlist_items (user_id, symbol, display_order) VALUES ('demo-user', 'RELIANCE', 0);
-- INSERT INTO watchlist_items (user_id, symbol, display_order) VALUES ('demo-user', 'TCS', 1);
-- INSERT INTO watchlist_items (user_id, symbol, display_order) VALUES ('demo-user', 'INFY', 2);
