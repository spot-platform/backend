-- ============================================================================
-- Migration: ChatRoomMember 도입 + ChatRoom 컬럼 확장 + GROUP partial unique
-- Date     : 2026-05-15
-- Author   : 김동현
-- PR       : PR A — ChatRoomMember introduction
-- Target   : PostgreSQL (운영/로컬 동일 적용)
-- ============================================================================
--
-- 변경 사항:
--   1) chat_room_members 테이블 신규 생성 (멤버십 모델)
--      - UNIQUE (chat_room_id, user_id)
--      - INDEX  (user_id)  -- "내가 속한 방" 조회용
--   2) chat_rooms 에 name / image_url / post_id / is_deleted 컬럼 추가
--   3) chat_rooms 에 (spot_id) WHERE type='GROUP' AND is_deleted=false partial
--      unique index 추가 — "스팟당 GROUP 방 1 개" DB 수준 보장
--   4) 백필: 기존 chat_messages 의 sender 들을 해당 방의 멤버로 등록
--
-- ⚠️ ddl-auto=update 는 신규 테이블/컬럼 자동 생성 가능하지만 partial unique
--   index 와 백필 INSERT 는 자동 처리 안 됨 → 본 스크립트 수동 실행 필요.
--
-- 실행 절차 (로컬):
--   1. 서버 중지
--   2. psql -d backend_db -f docs/migrations/2026-05-15_chat_room_member.sql
--   3. 서버 재기동 — Hibernate 가 잔여 컬럼/제약을 idempotent 하게 맞춤
--
-- 롤백:
--   DROP INDEX IF EXISTS uq_chat_room_group_spot;
--   DROP TABLE IF EXISTS chat_room_members;
--   ALTER TABLE chat_rooms DROP COLUMN IF EXISTS name;
--   ALTER TABLE chat_rooms DROP COLUMN IF EXISTS image_url;
--   ALTER TABLE chat_rooms DROP COLUMN IF EXISTS post_id;
--   ALTER TABLE chat_rooms DROP COLUMN IF EXISTS is_deleted;
-- ============================================================================

BEGIN;

-- 1) ChatRoomMember 테이블
CREATE TABLE IF NOT EXISTS chat_room_members (
    id            BIGSERIAL PRIMARY KEY,
    chat_room_id  BIGINT       NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id       VARCHAR(255) NOT NULL,
    joined_at     TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_chat_room_member UNIQUE (chat_room_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_room_member_user
    ON chat_room_members(user_id);

-- 2) ChatRoom 컬럼 추가
ALTER TABLE chat_rooms ADD COLUMN IF NOT EXISTS name        VARCHAR(255);
ALTER TABLE chat_rooms ADD COLUMN IF NOT EXISTS image_url   VARCHAR(1024);
ALTER TABLE chat_rooms ADD COLUMN IF NOT EXISTS post_id     VARCHAR(36);
ALTER TABLE chat_rooms ADD COLUMN IF NOT EXISTS is_deleted  BOOLEAN NOT NULL DEFAULT false;

-- 3) GROUP 방은 (spot_id) 당 활성 1 개 — partial unique index
CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_room_group_spot
    ON chat_rooms (spot_id)
    WHERE type = 'GROUP' AND is_deleted = false AND spot_id IS NOT NULL;

-- 4) 백필: 기존 chat_messages 의 sender_id 를 해당 방 멤버로 등록.
--    한계: 메시지를 보낸 적이 없는 과거 멤버 (예: 입장만 하고 침묵한 유저) 는 본 백필에서
--    누락된다. 본 PR 이전 단계에는 멤버십 메타데이터가 존재하지 않았기 때문에 다른
--    백필 소스를 활용할 수 없다. 운영 환경에서 침묵 유저 명단이 다른 곳 (예: SpotParticipant)
--    에 남아 있다면 본 스크립트 외에 보강 백필을 별도 수행해야 한다.
--    현재 컴포지션 (chat 도메인 단독, 메시지 보낸 자만 사실상 참여자) 에서는 충분.
INSERT INTO chat_room_members (chat_room_id, user_id, joined_at)
SELECT m.room_id, m.sender_id, MIN(m.created_at)
  FROM chat_messages m
 WHERE m.sender_id IS NOT NULL
   AND m.sender_id <> ''
   AND NOT EXISTS (
        SELECT 1 FROM chat_room_members crm
         WHERE crm.chat_room_id = m.room_id
           AND crm.user_id      = m.sender_id
   )
 GROUP BY m.room_id, m.sender_id
ON CONFLICT (chat_room_id, user_id) DO NOTHING;

COMMIT;

-- 검증 쿼리:
-- SELECT count(*) FROM chat_room_members;
-- \d chat_rooms
-- \d chat_room_members
-- SELECT indexdef FROM pg_indexes WHERE indexname = 'uq_chat_room_group_spot';
