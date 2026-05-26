-- feed_items: 펀딩/파트너 집계 컬럼 추가
-- PR: feat(feed): T1 — FeedItem 필드 업그레이드 + Offer/Request 생성 API
-- 대응 엔티티: FeedItem.fundingGoal / fundedAmount / confirmedPartnerCount
--
-- 동일한 Hibernate ddl-auto:update 한계로 NOT NULL 컬럼 추가에 실패하므로
-- 서버 재기동 전에 수동으로 실행해야 한다.
-- 이미 적용된 환경에서는 IF NOT EXISTS 로 멱등하게 실행 가능.

ALTER TABLE feed_items
    ADD COLUMN IF NOT EXISTS funding_goal             INTEGER,
    ADD COLUMN IF NOT EXISTS funded_amount            INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS confirmed_partner_count  INTEGER NOT NULL DEFAULT 0;
