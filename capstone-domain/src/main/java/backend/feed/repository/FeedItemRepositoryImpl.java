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

@RequiredArgsConstructor
public class FeedItemRepositoryImpl implements FeedItemRepositoryCustom {

	private final JPAQueryFactory queryFactory;
	private final QFeedItem feedItem = QFeedItem.feedItem;

	@Override
	public Page<FeedItem> findAllByQuery(FeedListQuery query, Pageable pageable) {
		List<FeedItem> content = queryFactory
				.selectFrom(feedItem)
				.where(
						feedItem.deleted.isFalse(),
						eqType(query.getType()),
						eqStatus(query.getStatus()),
						eqCategory(query.getCategory())
				)
				.orderBy(getOrderSpecifier(query.getSort()))
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();

		Long total = queryFactory
				.select(feedItem.count())
				.from(feedItem)
				.where(
						feedItem.deleted.isFalse(),
						eqType(query.getType()),
						eqStatus(query.getStatus()),
						eqCategory(query.getCategory())
				)
				.fetchOne();

		return new PageImpl<>(content, pageable, total != null ? total : 0L);
	}

	private OrderSpecifier<?> getOrderSpecifier(String sort) {
		if (!StringUtils.hasText(sort)) {
			return feedItem.createdAt.desc();
		}
		return switch (sort) {
			case "popular" -> feedItem.views.desc();
			case "likes" -> feedItem.likes.desc();
			default -> feedItem.createdAt.desc();
		};
	}

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
