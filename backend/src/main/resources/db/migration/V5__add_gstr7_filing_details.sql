CREATE TABLE gstr7_filing_details (
    id BIGSERIAL PRIMARY KEY,
    gstin VARCHAR(15) NOT NULL,
    return_period VARCHAR(7) NOT NULL,
    due_date DATE,
    date_of_filing DATE,
    status VARCHAR(20),
    delay_days INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_gstin_period UNIQUE (gstin, return_period)
);
