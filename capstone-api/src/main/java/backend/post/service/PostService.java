package backend.post.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.global.enums.FeedItemStatus;
import backend.global.enums.PostType;
import backend.post.dto.CreateOfferPostRequest;
import backend.post.dto.CreateRequestPostRequest;
import backend.post.dto.PostCompletionResponse;
import backend.post.entity.Post;
import backend.post.repository.PostRepository;
import backend.spot.entity.Spot;
import backend.spot.repository.SpotRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

	private final PostRepository postRepository;
	private final SpotRepository spotRepository;

	/**
	 * Offer 게시글을 생성합니다.
	 */
	public PostCompletionResponse createOfferPost(CreateOfferPostRequest request) {
		Post post = Post.builder()
				.type(PostType.OFFER)
				.authorId("dummy-user-id") // 추후 인증 시스템 도입 시 실제 ID로 교체
				.authorNickname("황호찬")
				.spotName(request.getSpotName())
				.title(request.getTitle())
				.content(request.getContent())
				.categories(request.getCategories())
				.photoUrls(request.getPhotoUrls())
				.pointCost(request.getPointCost())
				.detailDescription(request.getDetailDescription())
				.supporterPhotoUrl(request.getSupporterPhotoUrl())
				.build();

		Post savedPost = postRepository.save(post);

		return PostCompletionResponse.builder()
				.id(savedPost.getId())
				.type(savedPost.getType())
				.title(savedPost.getTitle())
				.redirectUrl("/posts/offer/" + savedPost.getId())
				.build();
	}

	/**
	 * Request 게시글을 생성합니다.
	 */
	public PostCompletionResponse createRequestPost(CreateRequestPostRequest request) {
		Post post = Post.builder()
				.type(PostType.REQUEST)
				.authorId("dummy-user-id")
				.authorNickname("황호찬")
				.spotName(request.getSpotName())
				.title(request.getTitle())
				.content(request.getContent())
				.categories(request.getCategories())
				.photoUrls(request.getPhotoUrls())
				.pointCost(request.getPointCost())
				.detailDescription(request.getDetailDescription())
				.serviceStylePhotoUrl(request.getServiceStylePhotoUrl())
				.build();

		Post savedPost = postRepository.save(post);

		return PostCompletionResponse.builder()
				.id(savedPost.getId())
				.type(savedPost.getType())
				.title(savedPost.getTitle())
				.redirectUrl("/posts/request/" + savedPost.getId())
				.build();
	}

	/**
	 * 게시글의 펀딩 조건을 확인하고, 성공 시 MATCHED 상태로 변경 후 Spot을 생성합니다.
	 *
	 * @param postId 처리할 게시글 ID
	 */
	public void checkAndMatchPost(String postId) {
		// 1. 게시글 조회 (없으면 예외 발생)
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("해당 게시글을 찾을 수 없습니다. id=" + postId));

		// 2. 이미 매칭된 게시글인지 확인 (중복 처리 방지)
		if (post.getStatus() == FeedItemStatus.MATCHED) {
			throw new IllegalStateException("이미 매칭이 완료된 게시글입니다.");
		}

		// 3. [비즈니스 로직] 여기에 펀딩 달성 조건(금액, 인원 등) 검사 로직이 들어갑니다.
		// 현재는 호출되면 무조건 매칭되는 시나리오로 작성합니다.
		boolean isFundingSuccess = true;

		if (isFundingSuccess) {
			// 4. 게시글 상태 변경 (OPEN -> MATCHED)
			post.match();

			// 5. 새로운 Spot 엔티티 생성 및 저장
			Spot newSpot = Spot.fromPost(
					post,
					post.getTitle(),
					post.getContent(),
					post.getPointCost()
			);

			// DB에 새로운 Spot을 저장합니다.
			spotRepository.save(newSpot);
		}
	}
}
