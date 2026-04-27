package backend.feed.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import backend.feed.dto.FeedListQuery;
import backend.feed.entity.FeedItem;

/**
 * QueryDSL 전용 메서드를 정의하는 인터페이스입니다.
 * 인터페이스 명칭 뒤에 'Custom'을 붙이는 것이 관례입니다.
 */
public interface FeedItemRepositoryCustom {
	Page<FeedItem> findAllByQuery(FeedListQuery query, Pageable pageable);
}
