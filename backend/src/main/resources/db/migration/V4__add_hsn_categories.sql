CREATE TABLE hsn_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE gstr7_hsn_master ADD COLUMN category_id BIGINT;
ALTER TABLE gstr7_hsn_master ADD CONSTRAINT fk_hsn_master_category
    FOREIGN KEY (category_id) REFERENCES hsn_category(id) ON DELETE SET NULL;

ALTER TABLE pan_hsn_config ADD COLUMN category_id BIGINT;
ALTER TABLE pan_hsn_config ADD CONSTRAINT fk_pan_hsn_category
    FOREIGN KEY (category_id) REFERENCES hsn_category(id) ON DELETE SET NULL;
ALTER TABLE pan_hsn_config DROP COLUMN hsn_code;

-- Seed initial "Scrap" category and link existing 7204 code
INSERT INTO hsn_category (name, description) VALUES ('Scrap', 'Scrap and waste materials');
UPDATE gstr7_hsn_master SET category_id = 1 WHERE hsn_code = '7204';
