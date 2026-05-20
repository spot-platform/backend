-- ============================================================================
-- Migration: 스팟 일정 모델 재설계 (단일 시각 → 슬롯/가용성)
-- Date     : 2026-05-20
-- Author   : 김동현
-- Issue    : #34 (F2) — SpotSchedule 구조 재설계
-- Target   : PostgreSQL
-- ============================================================================
--
-- 변경 사항:
--   1) 구 spot_schedules (id, spot_id, title, scheduled_at) 제거
--   2) spot_schedule_slots (spot_id, slot_date, slot_hour, confirmed) 신설
--      - UNIQUE (spot_id, slot_date, slot_hour)
--   3) spot_schedule_availabilities (slot_id, user_id) 신설
--      - UNIQUE (slot_id, user_id)
--
-- 의미:
--   FRONTEND.md SpotSchedule { proposedSlots[], confirmedSlot } 명세로 전환.
--   슬롯별 가용 사용자(availableUserIds)를 별도 테이블로 정규화.
--
-- ⚠️ 구 spot_schedules 데이터는 모델이 호환되지 않아 폐기된다(개발 단계).
--
-- 실행 절차:
--   psql -d backend_db -f docs/migrations/2026-05-20_spot_schedule_redesign.sql
--
-- 롤백:
--   DROP TABLE IF EXISTS spot_schedule_availabilities;
--   DROP TABLE IF EXISTS spot_schedule_slots;
-- ============================================================================

BEGIN;

DROP TABLE IF EXISTS spot_schedules;

CREATE TABLE IF NOT EXISTS spot_schedule_slots (
    id         BIGSERIAL    PRIMARY KEY,
    spot_id    VARCHAR(36)  NOT NULL,
    slot_date  DATE         NOT NULL,
    slot_hour  INTEGER      NOT NULL,
    confirmed  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_spot_schedule_slot UNIQUE (spot_id, slot_date, slot_hour)
);

CREATE INDEX IF NOT EXISTS idx_spot_schedule_slot_spot ON spot_schedule_slots(spot_id);

CREATE TABLE IF NOT EXISTS spot_schedule_availabilities (
    id      VARCHAR(36) PRIMARY KEY,
    slot_id BIGINT      NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    CONSTRAINT uq_spot_schedule_availability UNIQUE (slot_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_spot_schedule_availability_slot ON spot_schedule_availabilities(slot_id);

COMMIT;

-- 검증:
-- \d spot_schedule_slots
-- \d spot_schedule_availabilities
