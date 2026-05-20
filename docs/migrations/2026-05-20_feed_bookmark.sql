-- ============================================================================
-- Migration: feed_bookmarks 테이블 신규 생성
-- Date     : 2026-05-20
-- Author   : 황호찬
-- PR       : PR #48 — 피드 북마크 실구현 (#35)
-- Target   : PostgreSQL
-- ============================================================================
--
-- 변경 사항:
--   1) feed_bookmarks 테이블 생성 (userId, feedItemId, createdAt)
--      - PRIMARY KEY (id)
--      - UNIQUE (user_id, feed_item_id)  — 동일 피드 중복 북마크 방지
--      - INDEX (user_id)                 — "내 북마크 목록" 조회용
--
-- ⚠️ ddl-auto=update 로 엔티티만으로 테이블이 자동 생성되지만, 인덱스 일관성을
--   위해 본 스크립트 명시 적용 권장.
--
-- 실행 절차:
--   1. 서버 중지 (또는 테이블이 없는 경우 hot 실행 가능)
--   2. psql -d backend_db -f docs/migrations/2026-05-20_feed_bookmark.sql
--   3. 서버 재기동
--
-- 롤백:
--   DROP TABLE IF EXISTS feed_bookmarks;
-- ============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS feed_bookmarks (
    id           VARCHAR(36)  PRIMARY KEY,
    user_id      VARCHAR(36)  NOT NULL,
    feed_item_id VARCHAR(36)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_feed_bookmarks_user_feed UNIQUE (user_id, feed_item_id)
);

CREATE INDEX IF NOT EXISTS idx_feed_bookmarks_user
    ON feed_bookmarks(user_id);

COMMIT;

-- 검증:
-- \d feed_bookmarks
-- SELECT indexname FROM pg_indexes WHERE tablename = 'feed_bookmarks';
