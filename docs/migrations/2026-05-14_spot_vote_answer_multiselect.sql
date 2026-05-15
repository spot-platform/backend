-- ============================================================================
-- Migration: spot_vote_answers unique constraint replacement for multiSelect
-- Date     : 2026-05-14
-- Author   : 김동현
-- Issue    : PR #15 caveat — uq_vote_user (vote_id, user_id) blocks multiSelect.
-- Target   : PostgreSQL (운영/로컬 동일 적용)
-- ============================================================================
--
-- 배경:
--   초기 PR #8 에서 SpotVoteAnswer 에 unique constraint (vote_id, user_id) 를
--   추가하여 "1 유저 1 답변" 을 DB 수준에서 강제했음. PR #15 가 SpotVote 에
--   multiSelect 컬럼을 추가했지만 이 constraint 가 남아있는 한 다중 선택은
--   동작하지 않음.
--
-- 본 마이그레이션은 constraint 의미를 바꾼다:
--   기존  : (vote_id, user_id)        — 한 유저는 한 vote 에 한 옵션만 가능
--   변경  : (vote_id, user_id, option_id) — 같은 옵션 중복만 차단,
--                                          단일선택은 서비스 레이어가 보장
--
-- ⚠️  ddl-auto=update 는 기존 constraint 를 drop 하지 않으므로 수동 실행 필요.
--
-- 실행 절차 (로컬):
--   1. 서버 중지
--   2. psql -d backend_db -f docs/migrations/2026-05-14_spot_vote_answer_multiselect.sql
--   3. 서버 재기동 — Hibernate ddl-auto=update 가 새 constraint(uq_vote_user_option)를 생성함
--
-- 롤백:
--   ALTER TABLE spot_vote_answers DROP CONSTRAINT uq_vote_user_option;
--   ALTER TABLE spot_vote_answers ADD CONSTRAINT uq_vote_user UNIQUE (vote_id, user_id);
-- ============================================================================

BEGIN;

-- 1) 기존 constraint 제거
ALTER TABLE spot_vote_answers DROP CONSTRAINT IF EXISTS uq_vote_user;

-- 2) 새 constraint 추가 (Hibernate 가 동일 이름으로 만들어두면 중복 생성 방지)
ALTER TABLE spot_vote_answers
    ADD CONSTRAINT uq_vote_user_option UNIQUE (vote_id, user_id, option_id);

COMMIT;

-- 검증 쿼리:
-- \d spot_vote_answers
-- 또는
-- SELECT conname, pg_get_constraintdef(oid)
--   FROM pg_constraint
--  WHERE conrelid = 'spot_vote_answers'::regclass;
