-- Database Schema for GRC Score Application

CREATE TABLE IF NOT EXISTS gst_details (
    gstin VARCHAR(15) PRIMARY KEY,
    gst_type VARCHAR(50),
    trade_name VARCHAR(255),
    legal_name VARCHAR(255),
    registration_date DATE,
    gst_status VARCHAR(20),
    address TEXT,
    last_api_sync TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS gst_returns (
    id SERIAL PRIMARY KEY,
    gstin VARCHAR(15) REFERENCES gst_details(gstin),
    return_type VARCHAR(20), -- GSTR1, GSTR3B
    financial_year VARCHAR(10),
    tax_period VARCHAR(20),
    status VARCHAR(20), -- Filed, Not Filed
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS grc_score (
    id SERIAL PRIMARY KEY,
    gstin VARCHAR(15) REFERENCES gst_details(gstin),
    score DECIMAL(5, 2),
    score_version VARCHAR(10),
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gst_returns_gstin ON gst_returns(gstin);
CREATE INDEX idx_grc_score_gstin ON grc_score(gstin);
