-- ============================================================================
-- Migration: chat_blocks 테이블 신규 + 인덱스
-- Date     : 2026-05-18
-- Author   : 김동현
-- PR       : PR D — block users
-- Target   : PostgreSQL
-- ============================================================================
--
-- 변경 사항:
--   1) chat_blocks 테이블 생성 (blocker_id, blocked_id, created_at)
--      - UNIQUE (blocker_id, blocked_id)
--      - INDEX (blocker_id, created_at DESC) — 내 차단 목록 조회
--      - INDEX (blocked_id)                  — 양방향 검증 역조회 (createPersonalRoom)
--
-- 의미:
--   차단은 단방향 (A → B). 본 마이그레이션은 chat 도메인에 차단 row 만 추가하고,
--   기존 chat_messages / chat_room_members 는 그대로 둔다. 차단 효과는 application
--   레이어 (ChatService.getMessages, createPersonalRoom, countUnreadByUserAndRoomIds)
--   에서 row 의 created_at 시점 이후 메시지를 가리는 방식으로 적용된다 (Q2 정책).
--
-- ⚠️ ddl-auto=update 가 테이블/인덱스를 자동 생성하긴 하지만 인덱스 일관성을 위해
--   본 스크립트 명시 적용을 권장. (다른 PR 의 partial unique index 처럼 미적용
--   케이스가 있음.)
--
-- 실행 절차:
--   1. 서버 중지
--   2. psql -d backend_db -f docs/migrations/2026-05-18_chat_block.sql
--   3. 서버 재기동
--
-- 롤백:
--   DROP TABLE IF EXISTS chat_blocks;
-- ============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS chat_blocks (
    id          BIGSERIAL PRIMARY KEY,
    blocker_id  VARCHAR(255) NOT NULL,
    blocked_id  VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_chat_block_pair UNIQUE (blocker_id, blocked_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_block_blocker
    ON chat_blocks(blocker_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_block_blocked
    ON chat_blocks(blocked_id);

COMMIT;

-- 검증:
-- \d chat_blocks
-- SELECT indexname FROM pg_indexes WHERE tablename = 'chat_blocks';
