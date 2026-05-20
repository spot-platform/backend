ALTER TABLE feed_items
    ADD COLUMN lat  DOUBLE PRECISION NULL,
    ADD COLUMN lng  DOUBLE PRECISION NULL;

CREATE INDEX IF NOT EXISTS idx_feed_items_lat_lng
    ON feed_items(lat, lng)
    WHERE lat IS NOT NULL AND lng IS NOT NULL;
