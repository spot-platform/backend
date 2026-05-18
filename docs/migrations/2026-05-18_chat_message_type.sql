-- ============================================================================
-- Migration: chat_messages.type 컬럼 추가 (USER / SYSTEM)
-- Date     : 2026-05-18
-- Author   : 김동현
-- PR       : PR C — leave room + SYSTEM messages
-- Target   : PostgreSQL
-- ============================================================================
--
-- 변경 사항:
--   1) chat_messages 에 type VARCHAR (NOT NULL DEFAULT 'USER') 추가
--   2) 기존 row 는 모두 USER 로 분류 (default 가 자동 채움)
--
-- 배경:
--   PR C 에서 "OO 님이 나갔습니다" 같은 SYSTEM 메시지를 도입한다. 클라이언트가
--   일반 메시지와 시스템 안내를 구분해 렌더할 수 있도록 분류 컬럼이 필요.
--   senderId 컬럼은 NOT NULL 인 채로 두고 SYSTEM 메시지는 sentinel
--   senderId='SYSTEM' 으로 저장한다.
--
-- ⚠️ ddl-auto=update 가 컬럼 추가까지는 자동 처리하지만, CHECK 제약 (있다면)
--   까지 정확히 맞추진 않으므로 본 스크립트로 명시 적용 권장.
--
-- 실행 절차:
--   1. 서버 중지
--   2. psql -d backend_db -f docs/migrations/2026-05-18_chat_message_type.sql
--   3. 서버 재기동
--
-- 롤백:
--   ALTER TABLE chat_messages DROP COLUMN IF EXISTS type;
-- ============================================================================

BEGIN;

ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS type VARCHAR(20) NOT NULL DEFAULT 'USER';

-- enum 정확성을 DB 레벨에서도 강제 (Hibernate 자동 생성과 동등)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conrelid = 'chat_messages'::regclass
           AND conname = 'chat_message_type_check'
    ) THEN
        ALTER TABLE chat_messages
            ADD CONSTRAINT chat_message_type_check
            CHECK (type IN ('USER', 'SYSTEM'));
    END IF;
END $$;

COMMIT;

-- 검증:
-- SELECT type, count(*) FROM chat_messages GROUP BY type;
-- SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint
--  WHERE conrelid = 'chat_messages'::regclass AND contype = 'c';
