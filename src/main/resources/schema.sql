-- Database Schema for GRC Score Application

CREATE TABLE IF NOT EXISTS gst_details (
    gstin VARCHAR(15) PRIMARY KEY,
    gst_type VARCHAR(50),
    trade_name VARCHAR(255),
    legal_name VARCHAR(255),
    registration_date DATE,
    gst_status VARCHAR(100),
    address TEXT,
    last_api_sync TIMESTAMP,
    aggregate_turnover VARCHAR(255),
    delay_count_gstr1 INTEGER,
    delay_count_gstr3b INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE IF NOT EXISTS grc_score (
    gstin VARCHAR(15) PRIMARY KEY REFERENCES gst_details(gstin),
    score DECIMAL(5, 2),
    score_version VARCHAR(10),
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);




-- Force update columns to handle larger data
ALTER TABLE gst_details ALTER COLUMN gst_type TYPE VARCHAR(100);
ALTER TABLE gst_details ALTER COLUMN trade_name TYPE VARCHAR(500);
ALTER TABLE gst_details ALTER COLUMN legal_name TYPE VARCHAR(500);
ALTER TABLE gst_details ALTER COLUMN gst_status TYPE VARCHAR(100);
ALTER TABLE gst_details ALTER COLUMN aggregate_turnover TYPE VARCHAR(255);
