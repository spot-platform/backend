-- ============================================================================
-- Migration: spot_files 테이블에 size_bytes 컬럼 추가
-- Date     : 2026-05-19
-- Author   : 김동현
-- PR       : PR #32 — FRONTEND.md compliance
-- Target   : PostgreSQL
-- ============================================================================
--
-- 변경 사항:
--   1) spot_files.size_bytes  BIGINT  NULL  — 파일 크기(bytes)
--
-- 의미:
--   FRONTEND.md SharedFile.sizeBytes 명세 충족. 기존 row 는 NULL 유지되고,
--   신규 업로드 시 UploadFileRequest.sizeBytes 값이 저장된다. 클라이언트 측에서
--   파일 메타로 함께 보내도록 협의됨.
--
-- 실행 절차:
--   1. psql -d backend_db -f docs/migrations/2026-05-19_spot_file_size_bytes.sql
--   2. 서버 재기동 (선택)
--
-- 롤백:
--   ALTER TABLE spot_files DROP COLUMN IF EXISTS size_bytes;
-- ============================================================================

BEGIN;

ALTER TABLE spot_files
    ADD COLUMN IF NOT EXISTS size_bytes BIGINT;

COMMIT;

-- 검증:
-- \d spot_files
-- SELECT column_name, data_type, is_nullable
--   FROM information_schema.columns
--  WHERE table_name = 'spot_files'
--    AND column_name = 'size_bytes';
