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
import backend.global.enums.FeedType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FeedItemRepositoryImpl implements FeedItemRepositoryCustom {

	private final JPAQueryFactory queryFactory;
	private final QFeedItem feedItem = QFeedItem.feedItem;

	private static final double RADIUS_KM = 5.0;
	private static final double KM_PER_DEGREE_LAT = 111.0;

	@Override
	public Page<FeedItem> findAllByQuery(FeedListQuery query, Pageable pageable) {
		List<FeedItem> content = queryFactory
				.selectFrom(feedItem)
				.where(
						feedItem.deleted.isFalse(),
						eqType(query.getType()),
						eqStatus(query.getStatus()),
						eqCategory(query.getCategory()),
						eqIsAi(query.getIsAi()),
						nearLocation(query.getNearLat(), query.getNearLng())
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
						eqCategory(query.getCategory()),
						eqIsAi(query.getIsAi()),
						nearLocation(query.getNearLat(), query.getNearLng())
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

	private BooleanExpression eqType(FeedType type) {
		return type != null ? feedItem.type.eq(type) : null;
	}

	private BooleanExpression eqStatus(FeedItemStatus status) {
		return status != null ? feedItem.status.eq(status) : null;
	}

	private BooleanExpression eqCategory(FeedCategory category) {
		return category != null ? feedItem.category.eq(category) : null;
	}

	private BooleanExpression eqIsAi(Boolean isAi) {
		return isAi != null ? feedItem.isAi.eq(isAi) : null;
	}

	private BooleanExpression nearLocation(Double nearLat, Double nearLng) {
		if (nearLat == null || nearLng == null) {
			return null;
		}
		if (!Double.isFinite(nearLat) || !Double.isFinite(nearLng)
				|| nearLat < -90.0 || nearLat > 90.0
				|| nearLng < -180.0 || nearLng > 180.0) {
			throw new IllegalArgumentException("nearLat/nearLng out of range");
		}
		double latDelta = RADIUS_KM / KM_PER_DEGREE_LAT;
		double cosLat = Math.cos(Math.toRadians(nearLat));
		double safeCosLat = Math.max(Math.abs(cosLat), 1e-6);
		double lngDelta = RADIUS_KM / (KM_PER_DEGREE_LAT * safeCosLat);
		return feedItem.lat.isNotNull()
				.and(feedItem.lat.between(nearLat - latDelta, nearLat + latDelta))
				.and(feedItem.lng.between(nearLng - lngDelta, nearLng + lngDelta));
	}
}
