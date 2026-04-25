-- Add gstd_no column to gst_details
ALTER TABLE gst_details
    ADD COLUMN IF NOT EXISTS gstd_no VARCHAR(15);
