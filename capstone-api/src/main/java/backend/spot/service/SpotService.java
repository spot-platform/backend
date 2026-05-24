package backend.spot.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.OptimisticLockingFailureException;
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
import backend.spot.dto.CreateChecklistRequest;
import backend.spot.dto.CreateNoteRequest;
import backend.spot.dto.CreateReviewRequest;
import backend.spot.dto.CreateSettlementRequest;
import backend.spot.dto.CreateSpotRequest;
import backend.spot.dto.ScheduleSlotDto;
import backend.spot.dto.SettlementLineItemDto;
import backend.spot.dto.SpotChecklistResponse;
import backend.spot.dto.SpotDetailResponse;
import backend.spot.dto.SpotFileResponse;
import backend.spot.dto.SpotListResponse;
import backend.spot.dto.SpotMapItemResponse;
import backend.spot.dto.SpotNoteResponse;
import backend.spot.dto.SpotParticipantResponse;
import backend.spot.dto.SpotResponse;
import backend.spot.dto.SpotReviewResponse;
import backend.spot.dto.SpotScheduleResponse;
import backend.spot.dto.SpotSettlementResponse;
import backend.spot.dto.TimelineEventResponse;
import backend.spot.dto.UpdateScheduleRequest;
import backend.spot.dto.UploadFileRequest;
import backend.spot.entity.ParticipantRole;
import backend.spot.entity.ParticipantState;
import backend.spot.entity.Spot;
import backend.spot.entity.SpotChecklist;
import backend.spot.entity.SpotFile;
import backend.spot.entity.SpotNote;
import backend.spot.entity.SpotParticipant;
import backend.spot.entity.SpotReview;
import backend.spot.entity.SpotScheduleAvailability;
import backend.spot.entity.SpotScheduleSlot;
import backend.spot.entity.SpotSettlement;
import backend.spot.entity.SpotSettlementLineItem;
import backend.spot.entity.SpotTimelineEvent;
import backend.spot.entity.TimelineEventKind;
import backend.spot.entity.WorkflowApprovalStatus;
import backend.spot.repository.SpotChecklistRepository;
import backend.spot.repository.SpotFileRepository;
import backend.spot.repository.SpotNoteRepository;
import backend.spot.repository.SpotParticipantRepository;
import backend.spot.repository.SpotRepository;
import backend.spot.repository.SpotReviewRepository;
import backend.spot.repository.SpotScheduleAvailabilityRepository;
import backend.spot.repository.SpotScheduleSlotRepository;
import backend.spot.repository.SpotSettlementLineItemRepository;
import backend.spot.repository.SpotSettlementRepository;
import backend.spot.repository.SpotTimelineEventRepository;
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
	private final SpotChecklistRepository spotChecklistRepository;
	private final SpotFileRepository spotFileRepository;
	private final SpotNoteRepository spotNoteRepository;
	private final SpotTimelineEventRepository spotTimelineEventRepository;
	private final SpotReviewRepository spotReviewRepository;
	private final SpotSettlementRepository spotSettlementRepository;
	private final SpotSettlementLineItemRepository spotSettlementLineItemRepository;
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

		recordTimeline(saved.getId(), TimelineEventKind.CREATED, author.getId(), null);

		return toSpotResponse(saved);
	}

	/**
	 * 스팟 목록을 페이징하여 최신순으로 조회합니다.
	 */
	@Transactional(readOnly = true)
	public SpotListResponse getSpots(int page, int size, String currentUserId) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Spot> spotPage = spotRepository.findAll(pageable);

		Set<Long> owned = ownedSpotIds(currentUserId);
		List<SpotResponse> data = spotPage.getContent()
			.stream()
			// TODO: batch participant counts when N is large
			.map(spot -> toSpotResponse(spot, owned.contains(spot.getId())))
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
	public SpotListResponse searchSpots(String keyword, String scope, int page, int size, String currentUserId) {
		String normalizedScope = normalizeScope(scope);
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Spot> spotPage = spotRepository.searchByKeyword(keyword, normalizedScope, pageable);

		Set<Long> owned = ownedSpotIds(currentUserId);
		List<SpotResponse> data = spotPage.getContent().stream()
			.map(spot -> toSpotResponse(spot, owned.contains(spot.getId())))
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
	public SpotDetailResponse getSpot(Long spotId, String currentUserId) {
		Spot spot = findSpotOrThrow(spotId);
		long participantCount = spotParticipantRepository.countBySpotIdAndState(spotId, ParticipantState.ACTIVE);
		return SpotDetailResponse.of(
			spot, Math.toIntExact(participantCount), isOwner(spotId, currentUserId), loadTimeline(spotId));
	}

	private List<TimelineEventResponse> loadTimeline(Long spotId) {
		List<SpotTimelineEvent> events = spotTimelineEventRepository.findBySpotIdOrderByCreatedAtAscIdAsc(spotId);
		List<String> actorIds = events.stream().map(SpotTimelineEvent::getActorId).distinct().toList();
		Map<String, String> nicknameMap = userRepository.findAllByIdIn(actorIds).stream()
			.collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));
		return events.stream()
			.map(e -> TimelineEventResponse.of(e, nicknameMap.getOrDefault(e.getActorId(), e.getActorId())))
			.toList();
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

		recordTimeline(spotId, TimelineEventKind.MATCHED, spot.getAuthorId(), null);

		return toSpotResponse(spot);
	}

	/**
	 * 스팟을 취소합니다. (OPEN → CLOSED)
	 */
	public SpotResponse cancelSpot(Long spotId) {
		Spot spot = findSpotOrThrow(spotId);
		spot.cancel();
		chatService.closeGroupRoom(String.valueOf(spotId), "스팟이 취소되었습니다.");
		recordTimeline(spotId, TimelineEventKind.CANCELLED, spot.getAuthorId(), null);
		return toSpotResponse(spot);
	}

	/**
	 * 스팟을 완료 처리합니다. (MATCHED → CLOSED)
	 */
	public SpotResponse completeSpot(Long spotId) {
		Spot spot = findSpotOrThrow(spotId);
		spot.complete();
		chatService.closeGroupRoom(String.valueOf(spotId), "스팟이 완료되었습니다.");
		recordTimeline(spotId, TimelineEventKind.COMPLETED, spot.getAuthorId(), null);
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
		if (!spotParticipantRepository.existsBySpotIdAndUserIdAndState(
				spotId, userId, ParticipantState.ACTIVE)) {
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
		SpotNoteResponse response = SpotNoteResponse.of(spotNoteRepository.save(note), authorNickname);
		recordTimeline(spotId, TimelineEventKind.COMMENT, authorId, request.getContent());
		return response;
	}

	// ─────────────────────────────────────────────
	// 리뷰 (Review)
	// ─────────────────────────────────────────────

	/**
	 * 스팟에 작성된 후기 목록을 최신순으로 조회합니다.
	 */
	@Transactional(readOnly = true)
	public List<SpotReviewResponse> getReviews(Long spotId) {
		validateSpotExists(spotId);

		List<SpotReview> reviews = spotReviewRepository.findBySpotIdOrderByCreatedAtDesc(spotId);
		List<String> reviewerIds = reviews.stream().map(SpotReview::getReviewerId).distinct().toList();
		Map<String, String> nicknameMap = userRepository.findAllByIdIn(reviewerIds).stream()
			.collect(Collectors.toMap(UserEntity::getId, UserEntity::getNickname));
		return reviews.stream()
			.map(r -> SpotReviewResponse.of(r, nicknameMap.getOrDefault(r.getReviewerId(), r.getReviewerId())))
			.toList();
	}

	/**
	 * 스팟 후기를 작성합니다. 완료(CLOSED)된 스팟의 참여자만 작성 가능하며,
	 * 동일 대상에 대한 중복 후기는 거부합니다.
	 */
	public SpotReviewResponse createReview(Long spotId, CreateReviewRequest request, String currentUserId) {
		Spot spot = findSpotOrThrow(spotId);
		if (spot.getStatus() != FeedItemStatus.CLOSED) {
			throw new BusinessException(ErrorCode.SPOT_NOT_CLOSED);
		}
		validateParticipant(spotId, currentUserId, ErrorCode.NOT_SPOT_PARTICIPANT);

		if (spotReviewRepository.existsBySpotIdAndReviewerIdAndTargetNickname(
				spotId, currentUserId, request.getTargetNickname())) {
			throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
		}

		validateReviewTarget(spotId, request.getTargetNickname());

		SpotReview review = spotReviewRepository.save(SpotReview.builder()
			.spotId(spotId)
			.reviewerId(currentUserId)
			.targetNickname(request.getTargetNickname())
			.rating(request.getRating())
			.comment(request.getComment())
			.build());

		return SpotReviewResponse.of(review, lookupNickname(currentUserId));
	}

	/** 후기 대상 닉네임이 해당 스팟의 활성 참여자인지 검증한다. */
	private void validateReviewTarget(Long spotId, String targetNickname) {
		List<String> participantIds = spotParticipantRepository.findBySpotId(spotId).stream()
			.filter(p -> p.getState() == ParticipantState.ACTIVE)
			.map(SpotParticipant::getUserId)
			.toList();
		boolean isParticipantNickname = userRepository.findAllByIdIn(participantIds).stream()
			.map(UserEntity::getNickname)
			.anyMatch(nickname -> nickname.equals(targetNickname));
		if (!isParticipantNickname) {
			throw new BusinessException(ErrorCode.REVIEW_TARGET_NOT_PARTICIPANT);
		}
	}

	// ─────────────────────────────────────────────
	// 정산 (Settlement)
	// ─────────────────────────────────────────────

	/**
	 * 정산을 요청합니다. 완료(CLOSED)된 스팟의 작성자만 요청할 수 있으며,
	 * 항목 합계를 계산해 승인 대기(PENDING) 상태로 생성합니다.
	 */
	public SpotSettlementResponse requestSettlement(
		Long spotId, CreateSettlementRequest request, String currentUserId
	) {
		Spot spot = findSpotOrThrow(spotId);
		if (spot.getStatus() != FeedItemStatus.CLOSED) {
			throw new BusinessException(ErrorCode.SPOT_NOT_CLOSED);
		}
		if (!spot.getAuthorId().equals(currentUserId)) {
			throw new BusinessException(ErrorCode.NOT_SPOT_PARTICIPANT);
		}

		int totalAmount = request.getLineItems().stream()
			.mapToInt(SettlementLineItemDto::getAmount)
			.sum();

		SpotSettlement settlement = spotSettlementRepository.save(SpotSettlement.builder()
			.spotId(spotId)
			.requesterId(currentUserId)
			.summary(request.getSummary())
			.totalAmount(totalAmount)
			.build());

		List<SpotSettlementLineItem> items = request.getLineItems().stream()
			.map(dto -> SpotSettlementLineItem.builder()
				.settlementId(settlement.getId())
				.label(dto.getLabel())
				.amount(dto.getAmount())
				.build())
			.toList();
		spotSettlementLineItemRepository.saveAll(items);

		recordTimeline(spotId, TimelineEventKind.SETTLEMENT_REQUESTED, currentUserId, request.getSummary());

		return buildSettlementResponse(settlement);
	}

	/**
	 * 승인 대기 중인 정산을 승인 처리합니다. 스팟 참여자만 승인할 수 있습니다.
	 * (정산 합의 기록만 남기며, 포인트 이동은 수행하지 않습니다.)
	 */
	public SpotSettlementResponse approveSettlement(Long spotId, String currentUserId) {
		validateSpotExists(spotId);
		validateParticipant(spotId, currentUserId, ErrorCode.NOT_SPOT_PARTICIPANT);

		SpotSettlement settlement = spotSettlementRepository
			.findFirstBySpotIdOrderByCreatedAtDesc(spotId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
		if (settlement.getStatus() != WorkflowApprovalStatus.PENDING) {
			throw new BusinessException(ErrorCode.SETTLEMENT_NOT_PENDING);
		}

		settlement.approve();
		// 동시 승인 경합 시 @Version 충돌을 비즈니스 예외(이미 처리됨)로 변환.
		try {
			spotSettlementRepository.flush();
		} catch (OptimisticLockingFailureException e) {
			throw new BusinessException(ErrorCode.SETTLEMENT_NOT_PENDING);
		}
		recordTimeline(spotId, TimelineEventKind.SETTLEMENT_APPROVED, currentUserId, settlement.getSummary());

		return buildSettlementResponse(settlement);
	}

	private SpotSettlementResponse buildSettlementResponse(SpotSettlement settlement) {
		List<SettlementLineItemDto> lineItems = spotSettlementLineItemRepository
			.findBySettlementId(settlement.getId()).stream()
			.map(SettlementLineItemDto::from)
			.toList();
		return SpotSettlementResponse.of(settlement, lineItems);
	}

	// ─────────────────────────────────────────────
	// 내부 헬퍼
	// ─────────────────────────────────────────────

	private SpotResponse toSpotResponse(Spot spot) {
		return toSpotResponse(spot, false);
	}

	private SpotResponse toSpotResponse(Spot spot, boolean isOwner) {
		long participantCount = spotParticipantRepository.countBySpotIdAndState(
			spot.getId(),
			ParticipantState.ACTIVE
		);
		return SpotResponse.from(spot, Math.toIntExact(participantCount), isOwner);
	}

	/**
	 * 현재 사용자가 권한자(작성자 또는 참여자)인지 — 참여자 레코드 존재로 판정.
	 * (작성자는 AUTHOR 참여자, 매칭된 파트너/서포터는 PARTICIPANT 참여자)
	 */
	private boolean isOwner(Long spotId, String currentUserId) {
		return currentUserId != null
			&& spotParticipantRepository.existsBySpotIdAndUserIdAndState(
				spotId, currentUserId, ParticipantState.ACTIVE);
	}

	private Set<Long> ownedSpotIds(String currentUserId) {
		if (currentUserId == null) {
			return Set.of();
		}
		return new HashSet<>(spotParticipantRepository.findSpotIdsByUserIdAndState(
			currentUserId, ParticipantState.ACTIVE));
	}

	private void recordTimeline(Long spotId, TimelineEventKind kind, String actorId, String content) {
		spotTimelineEventRepository.save(SpotTimelineEvent.builder()
			.spotId(spotId)
			.kind(kind)
			.actorId(actorId)
			.content(content)
			.build());
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
