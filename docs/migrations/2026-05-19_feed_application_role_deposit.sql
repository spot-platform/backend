-- ============================================================================
-- Migration: feed_applications 테이블에 applied_role, deposit 컬럼 추가
-- Date     : 2026-05-19
-- Author   : 김동현
-- PR       : PR #32 — FRONTEND.md compliance
-- Target   : PostgreSQL
-- ============================================================================
--
-- 변경 사항:
--   1) feed_applications.applied_role  VARCHAR(20) NULL  — enum SUPPORTER|PARTNER
--   2) feed_applications.deposit       INTEGER     NULL  — 보증금(원)
--
-- 의미:
--   FRONTEND.md FeedApplication 타입 명세에 맞춘 컬럼 추가. 기존 row 는 NULL 로
--   유지되며, 신규 신청부터 채워진다. 추후 도메인 정책상 NOT NULL 로 강제하려면
--   별도 backfill + ALTER 필요.
--
-- ⚠️ ddl-auto=update 사용 환경에선 엔티티 변경만으로 컬럼이 자동 생성되지만,
--   배포 환경 일관성과 롤백 가능성을 위해 본 스크립트 명시 적용 권장.
--
-- 실행 절차:
--   1. 서버 중지 또는 hot ALTER (컬럼이 nullable 이므로 hot 가능)
--   2. psql -d backend_db -f docs/migrations/2026-05-19_feed_application_role_deposit.sql
--   3. 서버 재기동 (또는 그대로 유지)
--
-- 롤백:
--   ALTER TABLE feed_applications DROP COLUMN IF EXISTS applied_role;
--   ALTER TABLE feed_applications DROP COLUMN IF EXISTS deposit;
-- ============================================================================

BEGIN;

ALTER TABLE feed_applications
    ADD COLUMN IF NOT EXISTS applied_role VARCHAR(20);

ALTER TABLE feed_applications
    ADD COLUMN IF NOT EXISTS deposit INTEGER;

COMMIT;

-- 검증:
-- \d feed_applications
-- SELECT column_name, data_type, is_nullable
--   FROM information_schema.columns
--  WHERE table_name = 'feed_applications'
--    AND column_name IN ('applied_role', 'deposit');
