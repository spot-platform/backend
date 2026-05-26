-- chat_room_members: 방별 알림 수신 여부 컬럼 추가
-- PR: feat(chat): 방 알림 끄기(mute) 기능 (#103)
-- 대응 엔티티: ChatRoomMember.notificationEnabled
--
-- Hibernate ddl-auto:update 는 NOT NULL 컬럼을 기존 행이 있는 테이블에
-- DEFAULT 없이 추가하려다 PostgreSQL 오류로 실패한다.
-- 이 마이그레이션을 서버 재기동 전에 수동으로 실행해야 한다.

ALTER TABLE chat_room_members
    ADD COLUMN IF NOT EXISTS notification_enabled BOOLEAN NOT NULL DEFAULT TRUE;
