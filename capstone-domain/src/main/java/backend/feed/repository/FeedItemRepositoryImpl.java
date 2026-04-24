package backend.feed.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import backend.feed.dto.FeedListQuery;
import backend.feed.entity.FeedItem;
import backend.feed.entity.QFeedItem;
import backend.global.enums.FeedCategory;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.PostType;
import lombok.RequiredArgsConstructor;

/**
 * FeedItemRepositoryCustom의 실제 구현체입니다.
 * QueryDSL을 사용하여 복잡한 동적 쿼리와 페이징을 처리합니다.
 */
@RequiredArgsConstructor
public class FeedItemRepositoryImpl implements FeedItemRepositoryCustom {

	private final JPAQueryFactory queryFactory;
	private final QFeedItem feedItem = QFeedItem.feedItem; // QueryDSL 전용 Q클래스

	@Override
	public Page<FeedItem> findAllByQuery(FeedListQuery query, Pageable pageable) {
		// 1. 메인 쿼리: 데이터 조회
		List<FeedItem> content = queryFactory
				.selectFrom(feedItem)
				.where(
						eqType(query.getType()),
						eqStatus(query.getStatus()),
						eqCategory(query.getCategory())
				)
				.orderBy(getOrderSpecifier(query.getSort())) // 동적 정렬 적용
				.offset(pageable.getOffset()) // 페이징 시작 지점
				.limit(pageable.getPageSize()) // 페이징 개수
				.fetch();

		// 2. 카운트 쿼리: 전체 개수 조회 (최적화를 위해 별도 실행)
		Long total = queryFactory
				.select(feedItem.count())
				.from(feedItem)
				.where(
						eqType(query.getType()),
						eqStatus(query.getStatus()),
						eqCategory(query.getCategory())
				)
				.fetchOne();

		return new PageImpl<>(content, pageable, total != null ? total : 0L);
	}

	/**
	 * 동적 정렬(Sorting)을 위한 OrderSpecifier 반환 메서드
	 */
	private OrderSpecifier<?> getOrderSpecifier(String sort) {
		if (!StringUtils.hasText(sort)) {
			return feedItem.createdAt.desc(); // 기본값: 최신순
		}

		return switch (sort) {
			case "popular" -> feedItem.views.desc(); // 인기순: 조회수 기반
			case "likes" -> feedItem.likes.desc();   // 추천순: 좋아요 기반
			default -> feedItem.createdAt.desc();    // 최신순
		};
	}

	/**
	 * [중요] BooleanExpression을 활용한 조건절 분리
	 * 파라미터가 null이면 null을 반환하고, QueryDSL의 where절은 null을 무시합니다.
	 * 이를 통해 동적 쿼리가 매우 깔끔해집니다.
	 */
	private BooleanExpression eqType(PostType type) {
		return type != null ? feedItem.type.eq(type) : null;
	}

	private BooleanExpression eqStatus(FeedItemStatus status) {
		return status != null ? feedItem.status.eq(status) : null;
	}

	private BooleanExpression eqCategory(FeedCategory category) {
		return category != null ? feedItem.category.eq(category) : null;
	}
}
