package backend.spot.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.chat.service.ChatService;
import backend.global.dto.ApiResponseMeta;
import backend.global.enums.FeedCategory;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.FeedType;
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import backend.spot.dto.CastVoteRequest;
import backend.spot.dto.CreateChecklistRequest;
import backend.spot.dto.CreateNoteRequest;
import backend.spot.dto.CreateSpotRequest;
import backend.spot.dto.CreateVoteRequest;
import backend.spot.dto.ScheduleSlotDto;
import backend.spot.dto.SpotChecklistResponse;
import backend.spot.dto.SpotFileResponse;
import backend.spot.dto.SpotListResponse;
import backend.spot.dto.SpotMapItemResponse;
import backend.spot.dto.SpotNoteResponse;
import backend.spot.dto.SpotParticipantResponse;
import backend.spot.dto.SpotResponse;
import backend.spot.dto.SpotScheduleResponse;
import backend.spot.dto.SpotVoteOptionResponse;
import backend.spot.dto.SpotVoteResponse;
import backend.spot.dto.SubmitVoteAnswersRequest;
import backend.spot.dto.UpdateScheduleRequest;
import backend.spot.dto.UploadFileRequest;
import backend.spot.entity.ParticipantRole;
import backend.spot.entity.ParticipantState;
import backend.spot.entity.Spot;
import backend.spot.entity.SpotChecklist;
import backend.spot.entity.SpotFile;
import backend.spot.entity.SpotNote;
import backend.spot.entity.SpotParticipant;
import backend.spot.entity.SpotScheduleAvailability;
import backend.spot.entity.SpotScheduleSlot;
import backend.spot.entity.SpotVote;
import backend.spot.entity.SpotVoteAnswer;
import backend.spot.entity.SpotVoteOption;
import backend.spot.entity.VoteState;
import backend.spot.repository.SpotChecklistRepository;
import backend.spot.repository.SpotFileRepository;
import backend.spot.repository.SpotNoteRepository;
import backend.spot.repository.SpotParticipantRepository;
import backend.spot.repository.SpotRepository;
import backend.spot.repository.SpotScheduleAvailabilityRepository;
import backend.spot.repository.SpotScheduleSlotRepository;
import backend.spot.repository.SpotVoteAnswerRepository;
import backend.spot.repository.SpotVoteOptionRepository;
import backend.spot.repository.SpotVoteRepository;
import backend.user.entity.UserEntity;
import backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SpotService {

	private final SpotRepository spotRepository;
	private final SpotParticipantRepository spotParticipantRepository;
	private final SpotScheduleSlotRepository spotScheduleSlotRepository;
	private final SpotScheduleAvailabilityRepository spotScheduleAvailabilityRepository;
	private final SpotVoteRepository spotVoteRepository;
	private final SpotVoteOptionRepository spotVoteOptionRepository;
	private final SpotVoteAnswerRepository spotVoteAnswerRepository;
	private final SpotChecklistRepository spotChecklistRepository;
	private final SpotFileRepository spotFileRepository;
	private final SpotNoteRepository spotNoteRepository;
	private final UserRepository userRepository;
	private final ChatService chatService;

	private static String resolveUserId(String currentUserId) {
		return currentUserId;
	}

	// ─────────────────────────────────────────────
	// Spot 기본 CRUD
	// ─────────────────────────────────────────────

	public SpotResponse createSpot(CreateSpotRequest request, String currentUserId) {
		UserEntity author = userRepository.findById(currentUserId)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		Spot spot = Spot.builder()
			.type(request.getType())
			.title(request.getTitle())
			.description(request.getDescription())
			.pointCost(request.getPointCost())
			.authorId(author.getId())
			.authorNickname(author.getNickname())
			.category(request.getCategory())
			.lat(request.getLat())
			.lng(request.getLng())
			.build();

		Spot saved = spotRepository.save(spot);

		// 작성자를 AUTHOR role 의 참가자로 동시 등록.
		spotParticipantRepository.save(
			SpotParticipant.builder()
				.spotId(saved.getId())
				.userId(author.getId())
				.role(ParticipantRole.AUTHOR)
				.state(ParticipantState.ACTIVE)
				.build()
		);

		// SPOT 생성 시점(OPEN)에 GROUP 채팅방을 즉시 개설하고 작성자를 첫 멤버로 등록.
		chatService.ensureGroupRoomForSpot(saved.getId().toString(), Set.of(author.getId()));

		return toSpotResponse(saved);
	}

	/**
	 * 스팟 목록을 페이징하여 최신순으로 조회합니다.
	 */
	@Transactional(readOnly = true)
	public SpotListResponse getSpots(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Spot> spotPage = spotRepository.findAll(pageable);

		List<SpotResponse> data = spotPage.getContent()
			.stream()
			.map(spot -> {
				// TODO: batch participant counts when N is large
				return toSpotResponse(spot);
			})
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
	 * 지도 마커용 스팟 목록을 조회합니다.
	 * bounds(sw/ne)는 4개 모두 주어질 때만 적용하며, 일부만 주어지면 400.
	 * type/status는 enum 문자열, 잘못된 값이면 400.
	 */
	@Transactional(readOnly = true)
	public List<SpotMapItemResponse> getSpotMap(
		Double swLat, Double swLng, Double neLat, Double neLng,
		String category, String type, String status
	) {
		boolean anyBounds = swLat != null || swLng != null || neLat != null || neLng != null;
		boolean allBounds = swLat != null && swLng != null && neLat != null && neLng != null;
		if (anyBounds && !allBounds) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}

		FeedType typeFilter = parseEnum(FeedType.class, type);
		FeedItemStatus statusFilter = parseEnum(FeedItemStatus.class, status);
		FeedCategory categoryFilter = parseEnum(FeedCategory.class, category);

		return spotRepository.findMapItems(swLat, swLng, neLat, neLng, typeFilter, statusFilter, categoryFilter)
			.stream()
			.map(SpotMapItemResponse::from)
			.toList();
	}

	/**
	 * 제목/설명 키워드로 스팟을 검색합니다.
	 */
	@Transactional(readOnly = true)
	public SpotListResponse searchSpots(String keyword, String scope, int page, int size) {
		String normalizedScope = normalizeScope(scope);
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Spot> spotPage = spotRepository.searchByKeyword(keyword, normalizedScope, pageable);

		List<SpotResponse> data = spotPage.getContent().stream()
			.map(this::toSpotResponse)
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

	private static String normalizeScope(String scope) {
		if (scope == null || scope.isBlank()) {
			return "ALL";
		}
		String upper = scope.toUpperCase();
		if (!upper.equals("ALL") && !upper.equals("TITLE") && !upper.equals("CONTENT")) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
		return upper;
	}

	private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Enum.valueOf(enumType, value);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}
	}

	/**
	 * 스팟 단건 상세 조회를 합니다.
	 */
	@Transactional(readOnly = true)
	public SpotResponse getSpot(Long spotId) {
		return toSpotResponse(findSpotOrThrow(spotId));
	}

	/**
	 * 스팟을 매칭 상태로 전환합니다. (OPEN → MATCHED)
	 */
	public SpotResponse matchSpot(Long spotId) {
		Spot spot = findSpotOrThrow(spotId);
		spot.match();

		// 채팅방은 createSpot 시점에 이미 개설됨. matchSpot 에서는 신규 참가자를 기존 방에 추가만 함.
		Set<String> memberUserIds = new HashSet<>();
		if (spot.getAuthorId() != null && !spot.getAuthorId().isBlank()) {
			memberUserIds.add(spot.getAuthorId());
		}
		spotParticipantRepository.findBySpotId(spotId).stream()
			.filter(p -> p.getState() == ParticipantState.ACTIVE)
			.map(SpotParticipant::getUserId)
			.filter(uid -> uid != null && !uid.isBlank())
			.forEach(memberUserIds::add);
		chatService.ensureGroupRoomForSpot(spotId.toString(), memberUserIds);

		return toSpotResponse(spot);
	}

	/**
	 * 스팟을 취소합니다. (OPEN → CLOSED)
	 */
	public SpotResponse cancelSpot(Long spotId) {
		Spot spot = findSpotOrThrow(spotId);
		spot.cancel();
		return toSpotResponse(spot);
	}

	/**
	 * 스팟을 완료 처리합니다. (MATCHED → CLOSED)
	 */
	public SpotResponse completeSpot(Long spotId) {
		Spot spot = findSpotOrThrow(spotId);
		spot.complete();
		return toSpotResponse(spot);
	}

	// ─────────────────────────────────────────────
	// 참여자 (Participant)
	// ─────────────────────────────────────────────

	/**
	 * 스팟에 참여 중인 유저 목록을 조회합니다.
	 */
	@Transactional(readOnly = true)
	public List<SpotParticipantResponse> getParticipants(Long spotId) {
		validateSpotExists(spotId);

		List<SpotParticipant> participants = spotParticipantRepository.findBySpotId(spotId);
		List<String> userIds = participants.stream().map(SpotParticipant::getUserId).toList();
		Map<String, String> nicknameMap = userRepository.findAllByIdIn(userIds).stream()
			.collect(Collectors.toMap(u -> u.getId(), u -> u.getNickname()));
		return participants.stream()
			.map(p -> SpotParticipantResponse.of(p, nicknameMap.getOrDefault(p.getUserId(), p.getUserId())))
			.toList();
	}

	// ─────────────────────────────────────────────
	// 일정 (Schedule)
	// ─────────────────────────────────────────────

	/**
	 * 스팟의 일정(제안 슬롯 + 확정 슬롯)을 조회합니다.
	 */
	@Transactional(readOnly = true)
	public SpotScheduleResponse getSchedule(Long spotId) {
		validateSpotExists(spotId);

		List<SpotScheduleSlot> slots = spotScheduleSlotRepository.findBySpotIdOrderBySlotDateAscSlotHourAsc(spotId);
		Map<Long, List<String>> availabilityBySlot = loadAvailabilities(slots);

		List<ScheduleSlotDto> proposed = slots.stream()
			.map(slot -> toSlotDto(slot, availabilityBySlot))
			.toList();
		ScheduleSlotDto confirmed = slots.stream()
			.filter(SpotScheduleSlot::isConfirmed)
			.findFirst()
			.map(slot -> toSlotDto(slot, availabilityBySlot))
			.orElse(null);

		return SpotScheduleResponse.builder()
			.spotId(spotId)
			.proposedSlots(proposed)
			.confirmedSlot(confirmed)
			.build();
	}

	/**
	 * 스팟 일정을 전체 교체합니다. (proposedSlots 로 슬롯/가용성 재구성, confirmedSlot 확정)
	 * confirmedSlot 은 proposedSlots 중 하나여야 합니다.
	 */
	@Transactional
	public SpotScheduleResponse updateSchedule(Long spotId, UpdateScheduleRequest request, String currentUserId) {
		validateSpotExists(spotId);
		validateParticipant(spotId, resolveUserId(currentUserId), ErrorCode.NOT_SPOT_PARTICIPANT);

		List<ScheduleSlotDto> proposed = request.getProposedSlots();
		ScheduleSlotDto confirmed = request.getConfirmedSlot();
		if (confirmed != null && proposed.stream().noneMatch(s -> sameSlot(s, confirmed))) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}

		// 중복 (date, hour) 슬롯 → 400
		long distinctCount = proposed.stream()
			.map(s -> s.getDate() + ":" + s.getHour())
			.distinct()
			.count();
		if (distinctCount != proposed.size()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
		}

		// availableUserIds 를 실제 참여자로만 제한
		Set<String> participantIds = spotParticipantRepository.findBySpotId(spotId).stream()
			.map(SpotParticipant::getUserId)
			.collect(Collectors.toSet());

		// 전체 교체: 기존 슬롯/가용성 벌크 삭제
		List<SpotScheduleSlot> existing = spotScheduleSlotRepository
			.findBySpotIdOrderBySlotDateAscSlotHourAsc(spotId);
		if (!existing.isEmpty()) {
			List<Long> existingIds = existing.stream().map(SpotScheduleSlot::getId).toList();
			spotScheduleAvailabilityRepository.deleteBySlotIdIn(existingIds);
			spotScheduleSlotRepository.deleteBySpotId(spotId);
		}

		List<SpotScheduleAvailability> allAvailabilities = new ArrayList<>();
		for (ScheduleSlotDto dto : proposed) {
			SpotScheduleSlot slot = spotScheduleSlotRepository.save(SpotScheduleSlot.builder()
				.spotId(spotId)
				.slotDate(dto.getDate())
				.slotHour(dto.getHour())
				.confirmed(confirmed != null && sameSlot(dto, confirmed))
				.build());

			dto.getAvailableUserIds().stream()
				.distinct()
				.filter(participantIds::contains)
				.map(userId -> SpotScheduleAvailability.builder()
					.slotId(slot.getId())
					.userId(userId)
					.build())
				.forEach(allAvailabilities::add);
		}
		if (!allAvailabilities.isEmpty()) {
			spotScheduleAvailabilityRepository.saveAll(allAvailabilities);
		}

		return getSchedule(spotId);
	}

	private Map<Long, List<String>> loadAvailabilities(List<SpotScheduleSlot> slots) {
		if (slots.isEmpty()) {
			return Map.of();
		}
		List<Long> slotIds = slots.stream().map(SpotScheduleSlot::getId).toList();
		return spotScheduleAvailabilityRepository.findBySlotIdIn(slotIds).stream()
			.collect(Collectors.groupingBy(
				SpotScheduleAvailability::getSlotId,
				Collectors.mapping(SpotScheduleAvailability::getUserId, Collectors.toList())));
	}

	private ScheduleSlotDto toSlotDto(SpotScheduleSlot slot, Map<Long, List<String>> availabilityBySlot) {
		return ScheduleSlotDto.builder()
			.date(slot.getSlotDate())
			.hour(slot.getSlotHour())
			.availableUserIds(new ArrayList<>(availabilityBySlot.getOrDefault(slot.getId(), List.of())))
			.build();
	}

	private boolean sameSlot(ScheduleSlotDto left, ScheduleSlotDto right) {
		return left.getDate().equals(right.getDate()) && left.getHour().equals(right.getHour());
	}

	// ─────────────────────────────────────────────
	// 투표 (Vote)
	// ─────────────────────────────────────────────

	/**
	 * 스팟의 투표 목록을 선택지 포함하여 조회합니다.
	 */
	@Transactional(readOnly = true)
	public List<SpotVoteResponse> getVotes(Long spotId, String currentUserId) {
		validateSpotExists(spotId);

		return spotVoteRepository.findBySpotIdOrderByCreatedAtDesc(spotId)
			.stream()
			.map(vote -> {
				List<SpotVoteAnswer> allAnswers = spotVoteAnswerRepository.findByVoteId(vote.getId());
				List<SpotVoteOptionResponse> options = spotVoteOptionRepository.findByVoteId(vote.getId())
					.stream()
					.map(opt -> {
						List<String> voterIds = allAnswers.stream()
							.filter(a -> a.getOptionId().equals(opt.getId()))
							.map(SpotVoteAnswer::getUserId)
							.toList();
						return SpotVoteOptionResponse.of(opt, voterIds);
					})
					.toList();
				return SpotVoteResponse.of(vote, options, getMyVotedOptionIds(vote.getId(), currentUserId));
			})
			.toList();
	}

	/**
	 * 스팟에 투표를 생성합니다.
	 * TODO: 인증 시스템 도입 후 creatorId 를 실제 로그인 유저 ID로 교체
	 */
	public SpotVoteResponse createVote(Long spotId, CreateVoteRequest request, String currentUserId) {
		validateSpotExists(spotId);

		SpotVote vote = SpotVote.builder()
			.spotId(spotId)
			.creatorId(resolveUserId(currentUserId))
			.question(request.getQuestion())
			.multiSelect(request.isMultiSelect())
			.build();

		SpotVote savedVote = spotVoteRepository.save(vote);

		List<SpotVoteOption> options = request.getOptions().stream()
			.map(content -> SpotVoteOption.builder()
				.voteId(savedVote.getId())
				.content(content)
				.build())
			.toList();

		List<SpotVoteOption> savedOptions = spotVoteOptionRepository.saveAll(options);

		List<SpotVoteOptionResponse> optionResponses = savedOptions.stream()
			.map(opt -> SpotVoteOptionResponse.of(opt, List.of()))
			.toList();

		return SpotVoteResponse.of(savedVote, optionResponses, getMyVotedOptionIds(savedVote.getId(), currentUserId));
	}

	/**
	 * 투표에 토글 시맨틱으로 참여/해제합니다. (카카오톡 투표 방식)
	 *
	 * <p>이미 해당 옵션에 투표한 상태에서 같은 옵션을 다시 캐스트하면 <b>투표 해제</b>되고
	 * voteCount 가 감소합니다. 별도 "투표 취소" 엔드포인트 없이 cast 한 개로 토글이 됩니다.
	 *
	 * <p>동작 규칙:
	 * <ul>
	 *   <li>이미 그 옵션에 답변이 있음 → 답변 삭제, count--</li>
	 *   <li>단일선택({@code multiSelect=false}) 이고 다른 옵션에 답변이 있음 →
	 *       기존 답변 삭제 + 카운트 감소 후 새 답변 추가 (= 표 변경)</li>
	 *   <li>그 외 → 새 답변 추가, count++</li>
	 * </ul>
	 *
	 * <p>검증:
	 * <ul>
	 *   <li>선택지 소속 검증: {@code optionId} 가 해당 {@code voteId} 에 속하는지 확인 (IDOR 방지)</li>
	 *   <li>원자적 카운트 증감: DB UPDATE 쿼리, voteCount 음수 가드</li>
	 *   <li>ACTIVE 상태가 아닌 투표는 거부</li>
	 *   <li>flush() 로 DELETE 를 INSERT 보다 먼저 실행시켜 unique constraint 충돌 방지</li>
	 * </ul>
	 */
	public SpotVoteResponse castVote(Long spotId, Long voteId, CastVoteRequest request, String currentUserId) {
		validateSpotExists(spotId);

		SpotVote vote = spotVoteRepository.findById(voteId)
			.filter(v -> v.getSpotId().equals(spotId))
			.orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

		if (vote.getState() != VoteState.ACTIVE) {
			throw new BusinessException(ErrorCode.VOTE_NOT_ACTIVE);
		}

		Long optionId = request.getOptionId();
		spotVoteOptionRepository.findById(optionId)
			.filter(o -> o.getVoteId().equals(voteId))
			.orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_IN_VOTE));

		String userId = resolveUserId(currentUserId);

		List<SpotVoteAnswer> myAnswers = spotVoteAnswerRepository.findAllByVoteIdAndUserId(voteId, userId);
		Optional<SpotVoteAnswer> existingOnSameOption = myAnswers.stream()
			.filter(a -> a.getOptionId().equals(optionId))
			.findFirst();

		if (existingOnSameOption.isPresent()) {
			// 토글 OFF: 같은 옵션 재캐스트 = 투표 해제
			spotVoteAnswerRepository.delete(existingOnSameOption.get());
			spotVoteAnswerRepository.flush();
			spotVoteOptionRepository.decrementVoteCount(optionId);
		} else {
			// 단일선택에서 다른 옵션에 이미 투표 중이면 기존 답변을 표 변경 처리
			if (!vote.isMultiSelect() && !myAnswers.isEmpty()) {
				spotVoteAnswerRepository.deleteAllByVoteIdAndUserId(voteId, userId);
				spotVoteAnswerRepository.flush();
				myAnswers.forEach(prev -> spotVoteOptionRepository.decrementVoteCount(prev.getOptionId()));
			}

			SpotVoteAnswer answer = SpotVoteAnswer.builder()
				.voteId(voteId)
				.optionId(optionId)
				.userId(userId)
				.build();
			spotVoteAnswerRepository.save(answer);
			spotVoteOptionRepository.incrementVoteCount(optionId);
		}

		List<SpotVoteAnswer> allAnswers = spotVoteAnswerRepository.findByVoteId(voteId);
		List<SpotVoteOptionResponse> optionResponses = spotVoteOptionRepository.findByVoteId(voteId)
			.stream()
			.map(opt -> {
				List<String> voterIds = allAnswers.stream()
					.filter(a -> a.getOptionId().equals(opt.getId()))
					.map(SpotVoteAnswer::getUserId)
					.toList();
				return SpotVoteOptionResponse.of(opt, voterIds);
			})
			.toList();

		return SpotVoteResponse.of(vote, optionResponses, getMyVotedOptionIds(vote.getId(), userId));
	}

	/**
	 * 투표 답변을 배치로 일괄 제출합니다. (카카오톡 다중선택 투표 UX: "선택중 → 투표 버튼")
	 *
	 * <p>요청 바디의 {@code optionIds} 가 <b>최종 확정 상태</b>이며, 서버가 현재 답변과 diff 하여
	 * 추가/삭제를 한 트랜잭션 안에서 원자적으로 적용합니다. 같은 body 를 두 번 보내면 변화 없음 (멱등).
	 *
	 * <p>동작 규칙:
	 * <ul>
	 *   <li>새로 추가된 옵션 → INSERT + voteCount++</li>
	 *   <li>제거된 옵션 → DELETE + voteCount--</li>
	 *   <li>그대로인 옵션 → no-op</li>
	 *   <li>빈 배열 {@code []} → 모든 답변 삭제 (= 투표 전체 취소)</li>
	 * </ul>
	 *
	 * <p>검증:
	 * <ul>
	 *   <li>ACTIVE 상태가 아닌 투표는 거부</li>
	 *   <li>모든 optionId 가 해당 voteId 소속이어야 함 (IDOR 방지)</li>
	 *   <li>단일선택({@code multiSelect=false}) 은 0~1개만 허용 → {@code SINGLE_SELECT_VOTE_LIMIT}</li>
	 *   <li>중복 optionId 는 자동 dedupe (Set)</li>
	 *   <li>flush() 로 DELETE 를 INSERT 보다 먼저 실행 (unique constraint 충돌 방지)</li>
	 * </ul>
	 */
	public SpotVoteResponse submitAnswers(
		Long spotId,
		Long voteId,
		SubmitVoteAnswersRequest request,
		String currentUserId
	) {
		validateSpotExists(spotId);

		SpotVote vote = spotVoteRepository.findById(voteId)
			.filter(v -> v.getSpotId().equals(spotId))
			.orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

		if (vote.getState() != VoteState.ACTIVE) {
			throw new BusinessException(ErrorCode.VOTE_NOT_ACTIVE);
		}

		Set<Long> desiredOptionIds = new HashSet<>(request.getOptionIds());

		if (!vote.isMultiSelect() && desiredOptionIds.size() > 1) {
			throw new BusinessException(ErrorCode.SINGLE_SELECT_VOTE_LIMIT);
		}

		// 모든 요청 optionId 가 이 vote 의 옵션인지 검증 (한 번의 쿼리로 묶어서)
		if (!desiredOptionIds.isEmpty()) {
			List<SpotVoteOption> allVoteOptions = spotVoteOptionRepository.findByVoteId(voteId);
			Set<Long> validOptionIds = allVoteOptions.stream()
				.map(SpotVoteOption::getId)
				.collect(java.util.stream.Collectors.toSet());
			for (Long optionId : desiredOptionIds) {
				if (!validOptionIds.contains(optionId)) {
					throw new BusinessException(ErrorCode.OPTION_NOT_IN_VOTE);
				}
			}
		}

		String userId = resolveUserId(currentUserId);

		List<SpotVoteAnswer> currentAnswers = spotVoteAnswerRepository.findAllByVoteIdAndUserId(voteId, userId);
		Set<Long> currentOptionIds = currentAnswers.stream()
			.map(SpotVoteAnswer::getOptionId)
			.collect(java.util.stream.Collectors.toSet());

		// diff: 제거 대상 (현재에는 있고 desired 에는 없음)
		List<SpotVoteAnswer> toRemove = currentAnswers.stream()
			.filter(a -> !desiredOptionIds.contains(a.getOptionId()))
			.toList();
		// diff: 추가 대상 (desired 에는 있고 현재에는 없음)
		List<Long> toAdd = desiredOptionIds.stream()
			.filter(id -> !currentOptionIds.contains(id))
			.toList();

		if (!toRemove.isEmpty()) {
			spotVoteAnswerRepository.deleteAll(toRemove);
			spotVoteAnswerRepository.flush();
			toRemove.forEach(a -> spotVoteOptionRepository.decrementVoteCount(a.getOptionId()));
		}

		for (Long optionId : toAdd) {
			spotVoteAnswerRepository.save(
				SpotVoteAnswer.builder()
					.voteId(voteId)
					.optionId(optionId)
					.userId(userId)
					.build()
			);
			spotVoteOptionRepository.incrementVoteCount(optionId);
		}

		List<SpotVoteAnswer> allAnswersAfter = spotVoteAnswerRepository.findByVoteId(voteId);
		List<SpotVoteOptionResponse> optionResponses = spotVoteOptionRepository.findByVoteId(voteId)
			.stream()
			.map(opt -> {
				List<String> voterIds = allAnswersAfter.stream()
					.filter(a -> a.getOptionId().equals(opt.getId()))
					.map(SpotVoteAnswer::getUserId)
					.toList();
				return SpotVoteOptionResponse.of(opt, voterIds);
			})
			.toList();

		return SpotVoteResponse.of(vote, optionResponses, getMyVotedOptionIds(vote.getId(), userId));
	}

	// ─────────────────────────────────────────────
	// 체크리스트 (Checklist)
	// ─────────────────────────────────────────────

	/**
	 * 스팟의 체크리스트 항목 목록을 조회합니다.
	 */
	@Transactional(readOnly = true)
	public List<SpotChecklistResponse> getChecklist(Long spotId) {
		validateSpotExists(spotId);

		List<SpotChecklist> items = spotChecklistRepository.findBySpotId(spotId);
		List<String> assigneeIds = items.stream()
			.map(SpotChecklist::getAssigneeId)
			.filter(id -> id != null)
			.distinct()
			.toList();
		Map<String, String> nicknameMap = userRepository.findAllByIdIn(assigneeIds).stream()
			.collect(Collectors.toMap(u -> u.getId(), u -> u.getNickname()));
		return items.stream()
			.map(item -> SpotChecklistResponse.of(item, resolveAssigneeNickname(item, nicknameMap)))
			.toList();
	}

	private String resolveAssigneeNickname(SpotChecklist item, Map<String, String> nicknameMap) {
		if (item.getAssigneeId() == null) {
			return null;
		}
		return nicknameMap.getOrDefault(item.getAssigneeId(), item.getAssigneeId());
	}

	/**
	 * 체크리스트 항목을 추가합니다. 담당자(assigneeId)가 주어지면 참여자인지 검증합니다.
	 */
	public SpotChecklistResponse addChecklistItem(Long spotId, CreateChecklistRequest request) {
		validateSpotExists(spotId);

		String assigneeId = request.getAssigneeId();
		if (assigneeId != null) {
			validateParticipant(spotId, assigneeId, ErrorCode.CHECKLIST_ASSIGNEE_NOT_PARTICIPANT);
		}

		SpotChecklist item = SpotChecklist.builder()
			.spotId(spotId)
			.content(request.getContent())
			.assigneeId(assigneeId)
			.build();

		SpotChecklist saved = spotChecklistRepository.save(item);
		return SpotChecklistResponse.of(saved, lookupNickname(assigneeId));
	}

	/**
	 * 체크리스트 항목의 완료 여부를 토글합니다.
	 * spotId 소속 검증으로 IDOR 를 방지합니다.
	 */
	public SpotChecklistResponse toggleChecklistItem(Long spotId, Long itemId) {
		SpotChecklist item = spotChecklistRepository.findById(itemId)
			.filter(i -> i.getSpotId().equals(spotId))
			.orElseThrow(() -> new BusinessException(ErrorCode.CHECKLIST_ITEM_NOT_FOUND));

		item.toggleDone();
		return SpotChecklistResponse.of(item, lookupNickname(item.getAssigneeId()));
	}

	/**
	 * 체크리스트 항목의 담당자를 지정하거나 해제합니다.
	 * 요청자는 스팟 참여자여야 하며, 지정 대상도 참여자여야 합니다. (assigneeId=null 이면 해제)
	 */
	public SpotChecklistResponse assignChecklistItem(
		Long spotId, Long itemId, String assigneeId, String currentUserId
	) {
		validateParticipant(spotId, resolveUserId(currentUserId), ErrorCode.NOT_SPOT_PARTICIPANT);

		SpotChecklist item = spotChecklistRepository.findById(itemId)
			.filter(i -> i.getSpotId().equals(spotId))
			.orElseThrow(() -> new BusinessException(ErrorCode.CHECKLIST_ITEM_NOT_FOUND));

		if (assigneeId != null) {
			validateParticipant(spotId, assigneeId, ErrorCode.CHECKLIST_ASSIGNEE_NOT_PARTICIPANT);
		}

		item.assignTo(assigneeId);
		return SpotChecklistResponse.of(item, lookupNickname(assigneeId));
	}

	private void validateParticipant(Long spotId, String userId, ErrorCode errorCode) {
		if (!spotParticipantRepository.existsBySpotIdAndUserId(spotId, userId)) {
			throw new BusinessException(errorCode);
		}
	}

	private String lookupNickname(String userId) {
		if (userId == null) {
			return null;
		}
		return userRepository.findById(userId).map(u -> u.getNickname()).orElse(userId);
	}

	// ─────────────────────────────────────────────
	// 파일 (File)
	// ─────────────────────────────────────────────

	/**
	 * 스팟에 등록된 파일 목록을 최신순으로 조회합니다.
	 */
	@Transactional(readOnly = true)
	public List<SpotFileResponse> getFiles(Long spotId) {
		validateSpotExists(spotId);

		List<SpotFile> files = spotFileRepository.findBySpotIdOrderByUploadedAtDesc(spotId);
		List<String> uploaderIds = files.stream().map(SpotFile::getUploaderId).toList();
		Map<String, String> nicknameMap = userRepository.findAllByIdIn(uploaderIds).stream()
			.collect(Collectors.toMap(u -> u.getId(), u -> u.getNickname()));
		return files.stream()
			.map(f -> SpotFileResponse.of(f, nicknameMap.getOrDefault(f.getUploaderId(), f.getUploaderId())))
			.toList();
	}

	/**
	 * 스팟에 파일 정보를 등록합니다. 업로더 식별은 인증된 유저 ID 를 사용합니다.
	 */
	public SpotFileResponse uploadFile(Long spotId, UploadFileRequest request, String currentUserId) {
		validateSpotExists(spotId);

		String uploaderId = resolveUserId(currentUserId);
		SpotFile file = SpotFile.builder()
			.spotId(spotId)
			.uploaderId(uploaderId)
			.fileName(request.getFileName())
			.fileUrl(request.getFileUrl())
			.sizeBytes(request.getSizeBytes())
			.build();

		String uploaderNickname = userRepository.findById(uploaderId)
			.map(u -> u.getNickname()).orElse(uploaderId);
		return SpotFileResponse.of(spotFileRepository.save(file), uploaderNickname);
	}

	/**
	 * 스팟에서 파일을 삭제합니다.
	 * spotId 소속 검증으로 IDOR 를 방지합니다.
	 */
	public void deleteFile(Long spotId, Long fileId) {
		SpotFile file = spotFileRepository.findById(fileId)
			.filter(f -> f.getSpotId().equals(spotId))
			.orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

		spotFileRepository.delete(file);
	}

	// ─────────────────────────────────────────────
	// 노트 (Note)
	// ─────────────────────────────────────────────

	/**
	 * 스팟의 노트 목록을 최신순으로 조회합니다.
	 */
	@Transactional(readOnly = true)
	public List<SpotNoteResponse> getNotes(Long spotId) {
		validateSpotExists(spotId);

		List<SpotNote> notes = spotNoteRepository.findBySpotIdOrderByCreatedAtDesc(spotId);
		List<String> authorIds = notes.stream().map(SpotNote::getAuthorId).toList();
		Map<String, String> nicknameMap = userRepository.findAllByIdIn(authorIds).stream()
			.collect(Collectors.toMap(u -> u.getId(), u -> u.getNickname()));
		return notes.stream()
			.map(n -> SpotNoteResponse.of(n, nicknameMap.getOrDefault(n.getAuthorId(), n.getAuthorId())))
			.toList();
	}

	/**
	 * 스팟에 노트를 작성합니다. 작성자 식별은 인증된 유저 ID 를 사용합니다.
	 */
	public SpotNoteResponse createNote(Long spotId, CreateNoteRequest request, String currentUserId) {
		validateSpotExists(spotId);

		String authorId = resolveUserId(currentUserId);
		SpotNote note = SpotNote.builder()
			.spotId(spotId)
			.authorId(authorId)
			.content(request.getContent())
			.build();

		String authorNickname = userRepository.findById(authorId)
			.map(u -> u.getNickname()).orElse(authorId);
		return SpotNoteResponse.of(spotNoteRepository.save(note), authorNickname);
	}

	// ─────────────────────────────────────────────
	// 내부 헬퍼
	// ─────────────────────────────────────────────

	private SpotResponse toSpotResponse(Spot spot) {
		long participantCount = spotParticipantRepository.countBySpotIdAndState(
			spot.getId(),
			ParticipantState.ACTIVE
		);
		return SpotResponse.from(spot, Math.toIntExact(participantCount));
	}

	private List<Long> getMyVotedOptionIds(Long voteId, String currentUserId) {
		if (currentUserId == null) {
			return null;
		}

		return spotVoteAnswerRepository.findAllByVoteIdAndUserId(voteId, currentUserId)
			.stream()
			.map(SpotVoteAnswer::getOptionId)
			.toList();
	}

	private Spot findSpotOrThrow(Long spotId) {
		return spotRepository.findById(spotId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SPOT_NOT_FOUND));
	}

	private void validateSpotExists(Long spotId) {
		if (!spotRepository.existsById(spotId)) {
			throw new BusinessException(ErrorCode.SPOT_NOT_FOUND);
		}
	}
}
