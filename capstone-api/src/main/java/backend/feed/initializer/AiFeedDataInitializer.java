package backend.feed.initializer;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import backend.feed.entity.FeedItem;
import backend.feed.repository.FeedItemRepository;
import backend.global.enums.FeedAuthorRole;
import backend.global.enums.FeedCategory;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.FeedType;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiFeedDataInitializer implements CommandLineRunner {

	private static final String FILE_PATH = "feed/ai_feed_items.json";

	private final FeedItemRepository feedItemRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public void run(String... args) throws IOException {
		if (feedItemRepository.existsByIsAi(true)) {
			return;
		}

		JsonNode root;
		try (InputStream is = new ClassPathResource(FILE_PATH).getInputStream()) {
			root = objectMapper.readTree(is);
		}

		JsonNode items = root.get("items");
		if (items == null || !items.isArray()) {
			throw new IllegalStateException("AI 피드 초기화 데이터 형식이 올바르지 않습니다. 'items' 배열이 필요합니다.");
		}

		java.util.List<FeedItem> feedItems = new java.util.ArrayList<>();
		for (JsonNode node : items) {
			try {
				feedItems.add(toFeedItem(node));
			} catch (Exception e) {
				// 개별 아이템 오류 시 로그 남기고 계속 진행 (Surgical change 원칙에 따라 로그는 간단히)
				System.err.println("AI 피드 아이템 파싱 실패: " + e.getMessage());
			}
		}

		if (!feedItems.isEmpty()) {
			feedItemRepository.saveAll(feedItems);
		}
	}

	private FeedItem toFeedItem(JsonNode node) {
		return FeedItem.builder()
			.spotId(longOrNull(node, "spot_id"))
			.title(requireText(node, "title"))
			.description(textOrNull(node, "description"))
			.location(requireText(node, "location"))
			.authorNickname(requireText(node, "authorNickname"))
			.authorRole(parseEnum(node, "authorRole", FeedAuthorRole.class))
			.price(requireInt(node, "price"))
			.type(parseEnum(node, "type", FeedType.class))
			.status(parseEnum(node, "status", FeedItemStatus.class))
			.category(parseCategory(node))
			.maxParticipants(intOrNull(node, "maxParticipants"))
			.isAi(true)
			.lat(doubleOrNull(node, "latitude"))
			.lng(doubleOrNull(node, "longitude"))
			.planJson(textOrNull(node, "planJson"))
			.priceBreakdownJson(textOrNull(node, "priceBreakdownJson"))
			.preparationJson(textOrNull(node, "preparationJson"))
			.venueAnchorsJson(textOrNull(node, "venueAnchorsJson"))
			.primaryPinJson(textOrNull(node, "primaryPinJson"))
			.build();
	}

	private FeedCategory parseCategory(JsonNode node) {
		JsonNode cat = node.get("category");
		if (cat == null || cat.isNull()) {
			return FeedCategory.기타;
		}
		try {
			return FeedCategory.valueOf(cat.asText());
		} catch (IllegalArgumentException e) {
			return FeedCategory.기타;
		}
	}

	private String requireText(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull() || value.asText().isBlank()) {
			throw new IllegalStateException("필수 텍스트 필드 누락: " + field);
		}
		return value.asText();
	}

	private int requireInt(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			throw new IllegalStateException("필수 숫자 필드 누락: " + field);
		}
		return value.asInt();
	}

	private <E extends Enum<E>> E parseEnum(JsonNode node, String field, Class<E> enumClass) {
		String text = requireText(node, field);
		try {
			return Enum.valueOf(enumClass, text);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("잘못된 열거형 값 (" + enumClass.getSimpleName() + "): " + text);
		}
	}

	private String textOrNull(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		return value.asText();
	}

	private Integer intOrNull(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		return value.asInt();
	}

	private Double doubleOrNull(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		return value.asDouble();
	}

	private Long longOrNull(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		return value.asLong();
	}
}
