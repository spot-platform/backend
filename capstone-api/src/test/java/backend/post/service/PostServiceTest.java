package backend.post.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import backend.feed.repository.FeedItemRepository;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.PostType;
import backend.post.entity.Post;
import backend.post.repository.PostRepository;
import backend.spot.entity.Spot;
import backend.spot.repository.SpotRepository;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

	@Mock
	private PostRepository postRepository;

	@Mock
	private FeedItemRepository feedItemRepository;

	@Mock
	private SpotRepository spotRepository;

	@InjectMocks
	private PostService postService;

	@Test
	@DisplayName("성공: OPEN 상태인 게시글에 convertToSpot 호출 시 MATCHED 전환 + Spot 저장된다.")
	void convertToSpot_Success() {
		String postId = "test-post-id";
		Post post = Post.builder()
				.id(postId)
				.type(PostType.OFFER)
				.status(FeedItemStatus.OPEN)
				.authorId("user-123")
				.authorNickname("호찬")
				.title("테스트 제목")
				.content("테스트 내용")
				.pointCost(5000)
				.build();

		given(postRepository.findById(postId)).willReturn(Optional.of(post));

		postService.convertToSpot(postId);

		assertEquals(FeedItemStatus.MATCHED, post.getStatus());
		verify(spotRepository, times(1)).save(any(Spot.class));
	}

	@Test
	@DisplayName("성공: 이미 MATCHED 상태이면 convertToSpot을 무시한다 (중복 방지).")
	void convertToSpot_Idempotent_AlreadyMatched() {
		String postId = "matched-id";
		Post post = Post.builder()
				.id(postId)
				.status(FeedItemStatus.MATCHED)
				.build();

		given(postRepository.findById(postId)).willReturn(Optional.of(post));

		postService.convertToSpot(postId);

		verify(spotRepository, never()).save(any(Spot.class));
	}

	@Test
	@DisplayName("실패: 존재하지 않는 ID로 convertToSpot 호출 시 예외 발생.")
	void convertToSpot_Fail_NotFound() {
		given(postRepository.findById("none")).willReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> postService.convertToSpot("none"));
	}
}
