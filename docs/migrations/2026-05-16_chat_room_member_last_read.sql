-- ============================================================================
-- Migration: chat_room_members.last_read_message_id 컬럼 추가 + 백필
-- Date     : 2026-05-16
-- Author   : 김동현
-- PR       : PR B — read tracking + real unreadCount
-- Target   : PostgreSQL
-- ============================================================================
--
-- 변경 사항:
--   1) chat_room_members 에 last_read_message_id (BIGINT, nullable) 추가
--   2) (chat_room_id, last_read_message_id) 인덱스 추가 — unread 카운트 쿼리 가속
--   3) 백필: 각 멤버의 last_read_message_id 를 해당 방의 최신 메시지 ID 로 설정
--      → 마이그레이션 시점 기준 "지금까지 본 거로 친다" 정책. 사용자가 배포 직후
--        수백 개의 unread 폭격을 받지 않도록.
--
-- ⚠️ ddl-auto=update 는 컬럼 추가까지는 자동 처리하지만 인덱스/백필은 수동 실행 필요.
--
-- 실행 절차:
--   1. 서버 중지
--   2. psql -d backend_db -f docs/migrations/2026-05-16_chat_room_member_last_read.sql
--   3. 서버 재기동
--
-- 롤백:
--   DROP INDEX IF EXISTS idx_chat_room_member_last_read;
--   ALTER TABLE chat_room_members DROP COLUMN IF EXISTS last_read_message_id;
-- ============================================================================

BEGIN;

-- 1) 컬럼 추가 (Hibernate ddl-auto=update 와 idempotent)
ALTER TABLE chat_room_members
    ADD COLUMN IF NOT EXISTS last_read_message_id BIGINT;

-- 2) unread 카운트 쿼리용 인덱스
CREATE INDEX IF NOT EXISTS idx_chat_room_member_last_read
    ON chat_room_members(chat_room_id, last_read_message_id);

-- 3) 백필 — 각 (room, user) 쌍에 대해 해당 방의 max(message_id) 로 셋팅
--    이미 last_read_message_id 가 있는 row 는 건드리지 않음 (재실행 안전).
UPDATE chat_room_members crm
   SET last_read_message_id = sub.max_id
  FROM (
       SELECT room_id AS chat_room_id, MAX(id) AS max_id
         FROM chat_messages
        GROUP BY room_id
       ) sub
 WHERE crm.chat_room_id = sub.chat_room_id
   AND crm.last_read_message_id IS NULL;

COMMIT;

-- 검증 쿼리:
-- SELECT chat_room_id, user_id, last_read_message_id FROM chat_room_members ORDER BY chat_room_id;
-- SELECT indexname FROM pg_indexes WHERE tablename = 'chat_room_members';
