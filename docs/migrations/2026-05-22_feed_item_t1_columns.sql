-- FeedItem T1 신규 컬럼 추가 (Post→Feed 통합, PR #62)
-- createOfferFeed / createRequestFeed API 전용 필드
ALTER TABLE feed_items
    ADD COLUMN spot_name             VARCHAR(255)  NULL,
    ADD COLUMN detail_description    TEXT          NULL,
    ADD COLUMN supporter_photo_url   VARCHAR(2048) NULL,
    ADD COLUMN service_style_photo_url VARCHAR(2048) NULL,
    ADD COLUMN categories_json       TEXT          NULL,
    ADD COLUMN photo_urls_json       TEXT          NULL;
