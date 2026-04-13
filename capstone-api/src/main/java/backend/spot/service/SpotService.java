package backend.spot.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.global.dto.ApiResponseMeta;
import backend.spot.dto.CreateSpotRequest;
import backend.spot.dto.SpotListResponse;
import backend.spot.dto.SpotResponse;
import backend.spot.entity.Spot;
import backend.spot.repository.SpotRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SpotService {

	private final SpotRepository spotRepository;

	/**
	 * 스팟을 생성합니다.
	 * TODO: 인증 시스템 도입 후 authorId, authorNickname 을 실제 로그인 유저 정보로 교체
	 */
	public SpotResponse createSpot(CreateSpotRequest request) {
		Spot spot = Spot.builder()
			.type(request.getType())
			.title(request.getTitle())
			.description(request.getDescription())
			.pointCost(request.getPointCost())
			.authorId("dummy-user-id")         // TODO: 실제 인증 유저 ID
			.authorNickname("테스트유저")        // TODO: 실제 인증 유저 닉네임
			.build();

		Spot saved = spotRepository.save(spot);
		return SpotResponse.from(saved);
	}

	/**
	 * 스팟 목록을 페이징하여 조회합니다.
	 * 최신순(createdAt 내림차순)으로 반환합니다.
	 *
	 * @param page 페이지 번호 (0부터 시작)
	 * @param size 한 페이지당 개수
	 */
	@Transactional(readOnly = true)
	public SpotListResponse getSpots(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Spot> spotPage = spotRepository.findAll(pageable);

		List<SpotResponse> data = spotPage.getContent()
			.stream()
			.map(SpotResponse::from)
			.toList();

		ApiResponseMeta meta = ApiResponseMeta.builder()
			.page(page)
			.size(size)
			.total(spotPage.getTotalElements())
			.hasNext(spotPage.hasNext())
			.build();

		return SpotListResponse.builder()
			.data(data)
			.meta(meta)
			.build();
	}

	/**
	 * 스팟 단건 상세 조회를 합니다.
	 *
	 * @param spotId 조회할 스팟 UUID
	 * @throws IllegalArgumentException 해당 ID의 스팟이 없을 경우
	 */
	@Transactional(readOnly = true)
	public SpotResponse getSpot(String spotId) {
		Spot spot = spotRepository.findById(spotId)
			.orElseThrow(() -> new IllegalArgumentException("해당 스팟을 찾을 수 없습니다. id=" + spotId));

		return SpotResponse.from(spot);
	}

	/**
	 * 스팟을 매칭 상태로 전환합니다. (OPEN → MATCHED)
	 * 참여자가 확정되어 스팟이 진행 중으로 전환될 때 호출됩니다.
	 *
	 * @param spotId 매칭할 스팟 UUID
	 */
	public SpotResponse matchSpot(String spotId) {
		Spot spot = spotRepository.findById(spotId)
			.orElseThrow(() -> new IllegalArgumentException("해당 스팟을 찾을 수 없습니다. id=" + spotId));

		spot.match(); // OPEN → MATCHED (엔티티 내부에서 상태 검증)
		return SpotResponse.from(spot);
	}

	/**
	 * 스팟을 취소합니다. (OPEN → CLOSED)
	 * 모집 중 취소될 때 호출됩니다.
	 *
	 * @param spotId 취소할 스팟 UUID
	 */
	public SpotResponse cancelSpot(String spotId) {
		Spot spot = spotRepository.findById(spotId)
			.orElseThrow(() -> new IllegalArgumentException("해당 스팟을 찾을 수 없습니다. id=" + spotId));

		spot.cancel(); // OPEN → CLOSED (엔티티 내부에서 상태 검증)
		return SpotResponse.from(spot);
	}

	/**
	 * 스팟을 완료 처리합니다. (MATCHED → CLOSED)
	 * 스팟 활동이 모두 끝났을 때 호출됩니다.
	 *
	 * @param spotId 완료할 스팟 UUID
	 */
	public SpotResponse completeSpot(String spotId) {
		Spot spot = spotRepository.findById(spotId)
			.orElseThrow(() -> new IllegalArgumentException("해당 스팟을 찾을 수 없습니다. id=" + spotId));

		spot.complete(); // MATCHED → CLOSED (엔티티 내부에서 상태 검증)
		return SpotResponse.from(spot);
	}
}
