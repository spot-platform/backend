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
import backend.global.enums.PostType;
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

		for (JsonNode node : root.get("items")) {
			feedItemRepository.save(toFeedItem(node));
		}
	}

	private FeedItem toFeedItem(JsonNode node) {
		return FeedItem.builder()
			.spotId(textOrNull(node, "spot_id"))
			.title(node.get("title").asText())
			.description(textOrNull(node, "description"))
			.location(node.get("location").asText())
			.authorNickname(node.get("authorNickname").asText())
			.authorRole(FeedAuthorRole.valueOf(node.get("authorRole").asText()))
			.price(node.get("price").asInt())
			.type(PostType.valueOf(node.get("type").asText()))
			.status(FeedItemStatus.valueOf(node.get("status").asText()))
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
}
