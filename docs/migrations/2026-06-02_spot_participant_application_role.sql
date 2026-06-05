-- ============================================================================
-- Migration: spot_participants 테이블에 application_role 컬럼 추가
-- Date     : 2026-06-02
-- Author   : Hermes
-- Issue    : #122 — expose spot member roles and settlement lookup
-- Target   : PostgreSQL
-- ============================================================================
--
-- 변경 사항:
--   1) spot_participants.application_role VARCHAR(20) NULL
--      - FeedApplication.applied_role(SUPPORTER|PARTNER)를 Spot 전환 후에도 보존한다.
--      - 작성자(AUTHOR)는 application_role=NULL 이며 API 응답에서 OWNER로 표시한다.
--
-- 의미:
--   기존 role(AUTHOR|PARTICIPANT)은 DB 호환성을 위해 유지하고, 프론트 표시용
--   OWNER/SUPPORTER/PARTNER는 role + application_role 조합으로 계산한다.
--
-- 실행 절차:
--   1. psql -d backend_db -f docs/migrations/2026-06-02_spot_participant_application_role.sql
--   2. 서버 재기동
--
-- 롤백:
--   ALTER TABLE spot_participants DROP COLUMN IF EXISTS application_role;
-- ============================================================================

BEGIN;

ALTER TABLE spot_participants
    ADD COLUMN IF NOT EXISTS application_role VARCHAR(20);

COMMIT;

-- 검증:
-- SELECT column_name, data_type, is_nullable
--   FROM information_schema.columns
--  WHERE table_name = 'spot_participants'
--    AND column_name = 'application_role';
