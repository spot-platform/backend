package backend.spot.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import backend.global.enums.FeedItemStatus;
import backend.global.enums.PostType;
import backend.spot.entity.Spot;

@Repository
public interface SpotRepository extends JpaRepository<Spot, String> {

	/**
	 * 지도 마커용 조회. 좌표가 있는 스팟만 반환하며,
	 * bounds(sw/ne) 와 type/status/category 는 값이 있을 때만 필터로 적용한다.
	 */
	@Query("""
		SELECT s FROM Spot s
		WHERE s.lat IS NOT NULL AND s.lng IS NOT NULL
			AND (:swLat IS NULL OR s.lat BETWEEN :swLat AND :neLat)
			AND (:swLng IS NULL OR s.lng BETWEEN :swLng AND :neLng)
			AND (:type IS NULL OR s.type = :type)
			AND (:status IS NULL OR s.status = :status)
			AND (:category IS NULL OR s.category = :category)
		""")
	List<Spot> findMapItems(
		@Param("swLat") Double swLat,
		@Param("swLng") Double swLng,
		@Param("neLat") Double neLat,
		@Param("neLng") Double neLng,
		@Param("type") PostType type,
		@Param("status") FeedItemStatus status,
		@Param("category") String category
	);

	/**
	 * 제목/설명 부분 일치 검색 (대소문자 무시).
	 */
	@Query("""
		SELECT s FROM Spot s
		WHERE LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
			OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
		""")
	Page<Spot> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
