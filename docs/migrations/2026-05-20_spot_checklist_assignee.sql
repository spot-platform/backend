-- ============================================================================
-- Migration: spot_checklists 테이블에 assignee_id 컬럼 추가
-- Date     : 2026-05-20
-- Author   : 김동현
-- Issue    : #39 (F5) — SpotChecklist 담당자
-- Target   : PostgreSQL
-- ============================================================================
--
-- 변경 사항:
--   1) spot_checklists.assignee_id  VARCHAR(36)  NULL  — 담당자 user id (논리 FK)
--
-- 의미:
--   FRONTEND.md ChecklistItem.assigneeId 명세 충족. 담당자 미지정 시 NULL.
--   닉네임(assigneeNickname)은 응답 시 user 조회로 채운다(컬럼 없음).
--
-- 실행 절차:
--   psql -d backend_db -f docs/migrations/2026-05-20_spot_checklist_assignee.sql
--
-- 롤백:
--   ALTER TABLE spot_checklists DROP COLUMN IF EXISTS assignee_id;
-- ============================================================================

BEGIN;

ALTER TABLE spot_checklists
    ADD COLUMN IF NOT EXISTS assignee_id VARCHAR(36);

COMMIT;

-- 검증:
-- SELECT column_name, data_type, is_nullable
--   FROM information_schema.columns
--  WHERE table_name = 'spot_checklists' AND column_name = 'assignee_id';
