-- Simulation place map-anchor metadata for manifest responses.
-- Safe to run repeatedly.

ALTER TABLE simulation_place
    ADD COLUMN IF NOT EXISTS map_anchor_json TEXT;

-- Rollback:
-- ALTER TABLE simulation_place DROP COLUMN IF EXISTS map_anchor_json;
