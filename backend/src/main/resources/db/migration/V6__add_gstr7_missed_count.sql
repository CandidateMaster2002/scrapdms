ALTER TABLE gst_details ADD COLUMN gstr7_missed_count integer DEFAULT 0;
ALTER TABLE gst_details ALTER COLUMN gstr7_status TYPE varchar(50);
