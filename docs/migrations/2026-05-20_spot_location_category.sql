-- ============================================================================
-- Migration: spots 테이블에 category, lat, lng 컬럼 추가
-- Date     : 2026-05-20
-- Author   : 김동현
-- Issue    : #37 (F3-c) — 스팟 지도 마커 / 검색
-- Target   : PostgreSQL
-- ============================================================================
--
-- 변경 사항:
--   1) spots.category  VARCHAR(50)       NULL  — 카테고리 (string, FRONTEND.md)
--   2) spots.lat       DOUBLE PRECISION  NULL  — 위도 (지도 마커)
--   3) spots.lng       DOUBLE PRECISION  NULL  — 경도 (지도 마커)
--
-- 의미:
--   FRONTEND.md SpotMapItem(coord, category) 명세 충족. 좌표가 있는 스팟만
--   /spots/map bounds 필터 대상이 된다. 기존 row 는 NULL (지도에 안 뜸).
--
-- 실행 절차:
--   psql -d backend_db -f docs/migrations/2026-05-20_spot_location_category.sql
--
-- 롤백:
--   ALTER TABLE spots DROP COLUMN IF EXISTS category;
--   ALTER TABLE spots DROP COLUMN IF EXISTS lat;
--   ALTER TABLE spots DROP COLUMN IF EXISTS lng;
-- ============================================================================

BEGIN;

ALTER TABLE spots
    ADD COLUMN IF NOT EXISTS category VARCHAR(50);

ALTER TABLE spots
    ADD COLUMN IF NOT EXISTS lat DOUBLE PRECISION;

ALTER TABLE spots
    ADD COLUMN IF NOT EXISTS lng DOUBLE PRECISION;

COMMIT;

-- 검증:
-- SELECT column_name, data_type FROM information_schema.columns
--  WHERE table_name = 'spots' AND column_name IN ('category', 'lat', 'lng');
