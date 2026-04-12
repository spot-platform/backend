package backend.post.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import backend.global.enums.FeedItemStatus;
import backend.global.enums.PostType;
import backend.post.entity.Post;
import backend.post.repository.PostRepository;
import backend.spot.entity.Spot;
import backend.spot.repository.SpotRepository;

@ExtendWith(MockitoExtension.class) // 가짜 객체(Mock) 기능을 사용하기 위한 설정입니다.
class PostServiceTest {

	@Mock // 가짜 Repository 객체를 만듭니다. (실제 DB에 접근하지 않습니다.)
	private PostRepository postRepository;

	@Mock
	private SpotRepository spotRepository;

	@InjectMocks // 가짜 객체들을 PostService에 주입(연결)해 줍니다.
	private PostService postService;

	@Test
	@DisplayName("성공: OPEN 상태인 게시글이 매칭되면 상태가 바뀌고 새로운 Spot이 저장된다.")
	void checkAndMatchPost_Success() {
		// [1] 가짜 데이터(Post) 준비
		String postId = "test-post-id";
		Post post = Post.builder()
				.id(postId)
				.type(PostType.OFFER)
				.status(FeedItemStatus.OPEN) // 처음에는 OPEN 상태
				.authorId("user-123")
				.authorNickname("호찬")
				.title("테스트 제목")
				.content("테스트 내용")
				.pointCost(5000)
				.build();

		// [2] 가짜 동작 설정 (Stubbing)
		// "postRepository에서 id로 조회하면 위에서 만든 post가 나온다"고 약속합니다.
		given(postRepository.findById(postId)).willReturn(Optional.of(post));

		// [3] 서비스 로직 실행 (테스트할 메서드 호출)
		postService.checkAndMatchPost(postId);

		// [4] 결과 검증 (Assertion)
		// - 게시글 상태가 MATCHED로 바뀌었는지 확인합니다.
		assertEquals(FeedItemStatus.MATCHED, post.getStatus());

		// - 새로운 Spot이 한 번 저장(save)되었는지 확인합니다.
		verify(spotRepository, times(1)).save(any(Spot.class));
	}

	@Test
	@DisplayName("실패: 이미 MATCHED 상태인 게시글을 매칭하려고 하면 에러가 발생한다.")
	void checkAndMatchPost_Fail_AlreadyMatched() {
		// [1] 이미 MATCHED 상태인 게시글 준비
		String postId = "matched-id";
		Post post = Post.builder()
				.id(postId)
				.status(FeedItemStatus.MATCHED)
				.build();

		given(postRepository.findById(postId)).willReturn(Optional.of(post));

		// [2] 실행 시 에러가 발생하는지 확인합니다.
		assertThrows(IllegalStateException.class, () -> {
			postService.checkAndMatchPost(postId);
		});
	}

	@Test
	@DisplayName("실패: 존재하지 않는 ID로 조회하면 에러가 발생한다.")
	void checkAndMatchPost_Fail_NotFound() {
		// [1] 없는 ID로 조회하도록 설정 (빈 상자 반환)
		String postId = "none";
		given(postRepository.findById(postId)).willReturn(Optional.empty());

		// [2] 실행 시 에러가 발생하는지 확인합니다.
		assertThrows(IllegalArgumentException.class, () -> {
			postService.checkAndMatchPost(postId);
		});
	}
}
