package backend.feed.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.feed.dto.FeedItemResponse;
import backend.feed.dto.FeedListQuery;
import backend.feed.dto.FeedListResponse;
import backend.feed.entity.FeedItem;
import backend.feed.repository.FeedItemRepository;
import backend.global.dto.ApiResponseMeta;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedItemService {

	private final FeedItemRepository feedItemRepository;

	/**
	 * QueryDSL을 사용하여 고성능 동적 필터링 피드 목록을 조회합니다.
	 */
	public FeedListResponse getFeedItems(FeedListQuery query) {
		// 1. 페이징 설정
		Pageable pageable = PageRequest.of(query.getPage(), query.getSize());

		// 2. QueryDSL 리포지토리 호출
		Page<FeedItem> feedItemPage = feedItemRepository.findAllByQuery(query, pageable);

		// 3. 결과를 DTO로 변환
		List<FeedItemResponse> content = feedItemPage.getContent().stream()
				.map(FeedItemResponse::from)
				.collect(Collectors.toList());

		// 4. 응답 구조 생성
		return FeedListResponse.builder()
				.data(content)
				.meta(ApiResponseMeta.builder()
						.page(feedItemPage.getNumber())
						.size(feedItemPage.getSize())
						.total(feedItemPage.getTotalElements())
						.hasNext(feedItemPage.hasNext())
						.build())
				.build();
	}
}
