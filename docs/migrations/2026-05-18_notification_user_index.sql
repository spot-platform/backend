-- notifications 테이블 user_id, created_at 복합 인덱스 추가
-- findByUserIdOrderByCreatedAtDesc 쿼리 최적화용
CREATE INDEX IF NOT EXISTS idx_notification_user_created
    ON notifications (user_id, created_at DESC);
