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

-- 구 테이블은 모델 비호환이므로 드롭 대신 백업 테이블로 보존 (롤백/데이터 보호)
ALTER TABLE IF EXISTS spot_schedules RENAME TO spot_schedules_backup_20260520;

CREATE TABLE IF NOT EXISTS spot_schedule_slots (
    id         BIGSERIAL    PRIMARY KEY,
    spot_id    VARCHAR(36)  NOT NULL,
    slot_date  DATE         NOT NULL,
    slot_hour  INTEGER      NOT NULL,
    confirmed  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_spot_schedule_slot UNIQUE (spot_id, slot_date, slot_hour),
    CONSTRAINT chk_spot_schedule_slot_hour CHECK (slot_hour BETWEEN 0 AND 23)
);

CREATE INDEX IF NOT EXISTS idx_spot_schedule_slot_spot ON spot_schedule_slots(spot_id);

-- 스팟당 확정 슬롯은 최대 1개 (confirmedSlot 싱글톤 계약을 DB 레벨에서 강제)
CREATE UNIQUE INDEX IF NOT EXISTS uq_spot_schedule_slot_confirmed_one_per_spot
    ON spot_schedule_slots(spot_id) WHERE confirmed = TRUE;

-- 가용성: 논리적 FK(애플리케이션 레이어 무결성, CAPSTONE 원칙) — 물리 FK 대신 인덱스만 둠
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
