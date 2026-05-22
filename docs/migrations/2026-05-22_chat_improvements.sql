-- ============================================================
-- Chat improvements migration
-- Branch: feature/chat-improvements
-- ============================================================

-- ① chat_rooms: 새 컬럼 추가
ALTER TABLE chat_rooms
    ADD COLUMN IF NOT EXISTS canonical_pair  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_read_only    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS updated_at      TIMESTAMP;

-- PERSONAL 방 중복 방지용 partial unique index
-- (동일한 두 유저 사이에 활성 PERSONAL 방은 1개만 허용)
CREATE UNIQUE INDEX IF NOT EXISTS uidx_chat_rooms_canonical_pair
    ON chat_rooms (canonical_pair)
    WHERE type = 'PERSONAL' AND is_deleted = FALSE;

-- updated_at 초기값 채우기 (기존 행)
UPDATE chat_rooms SET updated_at = created_at WHERE updated_at IS NULL;

-- ② chat_messages: 파일/이미지 메시지용 컬럼 추가
ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS file_url        TEXT,
    ADD COLUMN IF NOT EXISTS file_name       VARCHAR(255),
    ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT;
