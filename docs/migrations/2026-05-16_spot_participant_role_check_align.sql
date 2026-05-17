-- ============================================================================
-- Migration: spot_participants_role_check 제약을 코드 enum 과 정렬
-- Date     : 2026-05-16
-- Author   : 김동현
-- Target   : PostgreSQL
-- ============================================================================
--
-- 배경:
--   초기 스키마는 role 값으로 OWNER / PARTNER / SUPPORTER 를 가정하고 CHECK 제약을
--   걸어두었으나, 코드의 ParticipantRole enum 은 AUTHOR / PARTICIPANT 두 값만
--   사용한다. PR 'spot-author-as-owner-participant' 이전에는 spot_participants 에
--   row 가 거의 insert 되지 않아 불일치가 노출되지 않았지만, 본 PR 부터
--   createSpot 이 작성자를 AUTHOR role 로 자동 등록하므로 즉시 CHECK 위반이 발생한다.
--
-- 이 마이그레이션은:
--   1) 기존 spot_participants_role_check 제약을 제거하고
--   2) AUTHOR / PARTICIPANT 두 값만 허용하는 새 제약을 추가한다.
--
-- ⚠️ ddl-auto=update 는 CHECK 제약을 자동으로 갱신하지 않으므로 수동 실행 필요.
--
-- 실행 절차:
--   1. 서버 중지
--   2. psql -d backend_db -f docs/migrations/2026-05-16_spot_participant_role_check_align.sql
--   3. 서버 재기동
--
-- 롤백:
--   ALTER TABLE spot_participants DROP CONSTRAINT IF EXISTS spot_participant_role_check;
--   ALTER TABLE spot_participants
--     ADD CONSTRAINT spot_participants_role_check
--       CHECK (role::text IN ('OWNER', 'PARTNER', 'SUPPORTER'));
-- ============================================================================

BEGIN;

-- 1) 기존 제약 제거 (이름은 Hibernate 가 만든 자동 이름)
ALTER TABLE spot_participants DROP CONSTRAINT IF EXISTS spot_participants_role_check;

-- 2) 새 제약 — 코드 enum 과 1:1 매칭
ALTER TABLE spot_participants
    ADD CONSTRAINT spot_participant_role_check
    CHECK (role::text IN ('AUTHOR', 'PARTICIPANT'));

COMMIT;

-- 검증 쿼리:
-- SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint
--  WHERE conrelid = 'spot_participants'::regclass AND contype = 'c';
