-- SPOT simulation loop-overlap and hotspot metadata columns.
--
-- 대응 엔티티:
--   - SimulationRun.loopPeriodTicks / projectionTailTicks / maxProjectedTick
--   - SimulationLifecycleEvent.payloadJson / scheduledTick / scheduleLeadTicks
--     / durationTicks / expectedClosedAtTick / mapAnchorJson / hotspotSignalJson
--
-- ddl-auto:update 환경에서는 nullable 컬럼이 자동 추가될 수 있지만,
-- 스테이징/운영 배포 전 명시 적용할 수 있도록 멱등 SQL로 남긴다.
--
-- 롤백:
--   ALTER TABLE simulation_lifecycle_event DROP COLUMN IF EXISTS hotspot_signal_json;
--   ALTER TABLE simulation_lifecycle_event DROP COLUMN IF EXISTS map_anchor_json;
--   ALTER TABLE simulation_lifecycle_event DROP COLUMN IF EXISTS expected_closed_at_tick;
--   ALTER TABLE simulation_lifecycle_event DROP COLUMN IF EXISTS duration_ticks;
--   ALTER TABLE simulation_lifecycle_event DROP COLUMN IF EXISTS schedule_lead_ticks;
--   ALTER TABLE simulation_lifecycle_event DROP COLUMN IF EXISTS scheduled_tick;
--   ALTER TABLE simulation_lifecycle_event DROP COLUMN IF EXISTS payload_json;
--   ALTER TABLE simulation_run DROP COLUMN IF EXISTS max_projected_tick;
--   ALTER TABLE simulation_run DROP COLUMN IF EXISTS projection_tail_ticks;
--   ALTER TABLE simulation_run DROP COLUMN IF EXISTS loop_period_ticks;

BEGIN;

ALTER TABLE simulation_run
    ADD COLUMN IF NOT EXISTS loop_period_ticks INTEGER,
    ADD COLUMN IF NOT EXISTS projection_tail_ticks INTEGER,
    ADD COLUMN IF NOT EXISTS max_projected_tick INTEGER;

ALTER TABLE simulation_lifecycle_event
    ADD COLUMN IF NOT EXISTS payload_json TEXT,
    ADD COLUMN IF NOT EXISTS scheduled_tick INTEGER,
    ADD COLUMN IF NOT EXISTS schedule_lead_ticks INTEGER,
    ADD COLUMN IF NOT EXISTS duration_ticks INTEGER,
    ADD COLUMN IF NOT EXISTS expected_closed_at_tick INTEGER,
    ADD COLUMN IF NOT EXISTS map_anchor_json TEXT,
    ADD COLUMN IF NOT EXISTS hotspot_signal_json TEXT;

COMMIT;
