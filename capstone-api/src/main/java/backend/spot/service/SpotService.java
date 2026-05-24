package backend.spot.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import backend.notification.service.NotificationService;
import backend.spot.dto.CreateChecklistRequest;
import backend.spot.dto.CreateNoteRequest;
import backend.spot.dto.CreateSpotRequest;
import backend.spot.dto.ScheduleSlotDto;
import backend.spot.dto.SpotChecklistResponse;
import backend.spot.dto.SpotFileResponse;
import backend.spot.dto.SpotListResponse;
import backend.spot.dto.SpotMapItemResponse;
import backend.spot.dto.SpotNoteResponse;
import backend.spot.dto.SpotParticipantResponse;
import backend.spot.dto.SpotResponse;
import backend.spot.dto.SpotScheduleResponse;
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
import backend.spot.repository.SpotChecklistRepository;
import backend.spot.repository.SpotFileRepository;
import backend.spot.repository.SpotNoteRepository;
import backend.spot.repository.SpotParticipantRepository;
import backend.spot.repository.SpotRepository;
import backend.spot.repository.SpotScheduleAvailabilityRepository;
import backend.spot.repository.SpotScheduleSlotRepository;
import backend.user.entity.UserEntity;
import backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
	private final UserRepository userRepository;
	private final ChatService chatService;
	private final NotificationService notificationService;

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
	public SpotResponse getSpot(Long spotId, String currentUserId) {
		Spot spot = findSpotOrThrow(spotId);
		return toSpotResponse(spot, isOwner(spotId, currentUserId));
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
		memberUserIds.forEach(uid -> {
			try {
				notificationService.sendAfterCommit(uid, "'" + spot.getTitle() + "' 매칭이 확정됐어요");
			} catch (Exception e) {
				log.warn("[notification] 스팟 매칭 알림 전송 실패 - spotId={}, userId={}, error={}", spotId, uid, e.getMessage());
			}
		});

		return toSpotResponse(spot);
	}

	/**
	 * 스팟을 취소합니다. (OPEN → CLOSED)
	 */
	public SpotResponse cancelSpot(Long spotId) {
		Spot spot = findSpotOrThrow(spotId);
		spot.cancel();
		chatService.closeGroupRoom(String.valueOf(spotId), "스팟이 취소되었습니다.");
		getActiveMemberIds(spotId, spot).forEach(uid -> {
			try {
				notificationService.sendAfterCommit(uid, "'" + spot.getTitle() + "'이 취소됐어요");
			} catch (Exception e) {
				log.warn("[notification] 스팟 취소 알림 전송 실패 - spotId={}, userId={}, error={}", spotId, uid, e.getMessage());
			}
		});
		return toSpotResponse(spot);
	}

	/**
	 * 스팟을 완료 처리합니다. (MATCHED → CLOSED)
	 */
	public SpotResponse completeSpot(Long spotId) {
		Spot spot = findSpotOrThrow(spotId);
		spot.complete();
		chatService.closeGroupRoom(String.valueOf(spotId), "스팟이 완료되었습니다.");
		getActiveMemberIds(spotId, spot).forEach(uid -> {
			try {
				notificationService.sendAfterCommit(uid, "'" + spot.getTitle() + "' 활동이 완료됐어요. 리뷰를 남겨주세요!");
			} catch (Exception e) {
				log.warn("[notification] 스팟 완료 알림 전송 실패 - spotId={}, userId={}, error={}", spotId, uid, e.getMessage());
			}
		});
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
		Spot spot = findSpotOrThrow(spotId);
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

		if (confirmed != null) {
			getActiveMemberIds(spotId, spot).forEach(uid -> {
				try {
					notificationService.sendAfterCommit(uid, "'" + spot.getTitle() + "' 일정이 확정됐어요");
				} catch (Exception e) {
					log.warn("[notification] 스팟 일정 확정 알림 전송 실패 - spotId={}, userId={}, error={}", spotId, uid, e.getMessage());
				}
			});
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
		if (assigneeId != null && !assigneeId.equals(currentUserId)) {
			try {
				notificationService.sendAfterCommit(assigneeId,
						"'" + item.getContent() + "' 담당자로 지정됐어요");
			} catch (Exception e) {
				log.warn("[notification] 체크리스트 담당자 알림 전송 실패 - itemId={}, error={}", itemId, e.getMessage());
			}
		}
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
		return SpotNoteResponse.of(spotNoteRepository.save(note), authorNickname);
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

	private Spot findSpotOrThrow(Long spotId) {
		return spotRepository.findById(spotId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SPOT_NOT_FOUND));
	}

	private void validateSpotExists(Long spotId) {
		if (!spotRepository.existsById(spotId)) {
			throw new BusinessException(ErrorCode.SPOT_NOT_FOUND);
		}
	}

	private Set<String> getActiveMemberIds(Long spotId, Spot spot) {
		Set<String> ids = new HashSet<>();
		if (spot.getAuthorId() != null && !spot.getAuthorId().isBlank()) {
			ids.add(spot.getAuthorId());
		}
		spotParticipantRepository.findBySpotId(spotId).stream()
			.filter(p -> p.getState() == ParticipantState.ACTIVE)
			.map(SpotParticipant::getUserId)
			.filter(uid -> uid != null && !uid.isBlank())
			.forEach(ids::add);
		return ids;
	}
}
