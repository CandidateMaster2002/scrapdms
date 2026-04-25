CREATE TABLE gstr7_hsn_master (
    hsn_code VARCHAR(10) PRIMARY KEY,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- Seed with the initial requirement
INSERT INTO gstr7_hsn_master (hsn_code, description) VALUES ('7204', 'Ferrous waste and scrap');
