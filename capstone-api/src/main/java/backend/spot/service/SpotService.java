package backend.spot.service;

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
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import backend.spot.dto.CastVoteRequest;
import backend.spot.dto.CreateChecklistRequest;
import backend.spot.dto.CreateNoteRequest;
import backend.spot.dto.CreateScheduleRequest;
import backend.spot.dto.CreateSpotRequest;
import backend.spot.dto.CreateVoteRequest;
import backend.spot.dto.SpotChecklistResponse;
import backend.spot.dto.SpotFileResponse;
import backend.spot.dto.SpotListResponse;
import backend.spot.dto.SpotNoteResponse;
import backend.spot.dto.SpotParticipantResponse;
import backend.spot.dto.SpotResponse;
import backend.spot.dto.SpotScheduleResponse;
import backend.spot.dto.SpotVoteOptionResponse;
import backend.spot.dto.SpotVoteResponse;
import backend.spot.dto.SubmitVoteAnswersRequest;
import backend.spot.dto.UploadFileRequest;
import backend.spot.entity.ParticipantRole;
import backend.spot.entity.ParticipantState;
import backend.spot.entity.Spot;
import backend.spot.entity.SpotChecklist;
import backend.spot.entity.SpotFile;
import backend.spot.entity.SpotNote;
import backend.spot.entity.SpotParticipant;
import backend.spot.entity.SpotSchedule;
import backend.spot.entity.SpotVote;
import backend.spot.entity.SpotVoteAnswer;
import backend.spot.entity.SpotVoteOption;
import backend.spot.entity.VoteState;
import backend.spot.repository.SpotChecklistRepository;
import backend.spot.repository.SpotFileRepository;
import backend.spot.repository.SpotNoteRepository;
import backend.spot.repository.SpotParticipantRepository;
import backend.spot.repository.SpotRepository;
import backend.spot.repository.SpotScheduleRepository;
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
	private final SpotScheduleRepository spotScheduleRepository;
	private final SpotVoteRepository spotVoteRepository;
	private final SpotVoteOptionRepository spotVoteOptionRepository;
	private final SpotVoteAnswerRepository spotVoteAnswerRepository;
	private final SpotChecklistRepository spotChecklistRepository;
	private final SpotFileRepository spotFileRepository;
	private final SpotNoteRepository spotNoteRepository;
	private final UserRepository userRepository;
	private final ChatService chatService;

	private static final String FALLBACK_USER_ID = "dummy-user-id";
	private static final String FALLBACK_NICKNAME = "테스트유저";

	private static String resolveUserId(String currentUserId) {
		return (currentUserId != null && !currentUserId.isBlank()) ? currentUserId : FALLBACK_USER_ID;
	}

	// ─────────────────────────────────────────────
	// Spot 기본 CRUD
	// ─────────────────────────────────────────────

	/**
	 * 스팟을 생성합니다.
	 *
	 * <p>인증된 유저의 userId 와 nickname 을 작성자 정보로 저장합니다.
	 * 미인증 호출(인증 미적용 단계)에는 fallback 더미 값을 사용합니다.
	 * authorNickname 은 스냅샷이라 작성 시점 닉네임을 보존합니다.
	 */
	public SpotResponse createSpot(CreateSpotRequest request, String currentUserId) {
		UserEntity author = currentUserId == null ? null : userRepository.findById(currentUserId).orElse(null);

		// authorId/authorNickname 을 항상 같은 source(author 객체) 에서 도출.
		// userRepository.findById 가 miss 했을 때 한쪽만 실제 ID 가 들어가는 불일치를 방지.
		String authorId = author != null ? author.getId() : FALLBACK_USER_ID;
		Spot spot = Spot.builder()
			.type(request.getType())
			.title(request.getTitle())
			.description(request.getDescription())
			.pointCost(request.getPointCost())
			.authorId(authorId)
			.authorNickname(author != null ? author.getNickname() : FALLBACK_NICKNAME)
			.build();

		Spot saved = spotRepository.save(spot);

		// 작성자를 AUTHOR role 의 참가자로 동시 등록.
		spotParticipantRepository.save(
			SpotParticipant.builder()
				.spotId(saved.getId())
				.userId(authorId)
				.role(ParticipantRole.AUTHOR)
				.state(ParticipantState.ACTIVE)
				.build()
		);

		// SPOT 생성 시점(OPEN)에 GROUP 채팅방을 즉시 개설하고 작성자를 첫 멤버로 등록.
		// 기존에는 MATCHED 전환 시 채팅방을 열었으나, OPEN 상태에서도 토론·조율이 필요하므로
		// 생성 즉시 채팅방을 보장한다. matchSpot 은 신규 참가자 가입만 처리.
		chatService.ensureGroupRoomForSpot(saved.getId(), Set.of(authorId));

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
	 * 스팟 단건 상세 조회를 합니다.
	 */
	@Transactional(readOnly = true)
	public SpotResponse getSpot(String spotId) {
		return toSpotResponse(findSpotOrThrow(spotId));
	}

	/**
	 * 스팟을 매칭 상태로 전환합니다. (OPEN → MATCHED)
	 */
	public SpotResponse matchSpot(String spotId) {
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
		chatService.ensureGroupRoomForSpot(spotId, memberUserIds);

		return toSpotResponse(spot);
	}

	/**
	 * 스팟을 취소합니다. (OPEN → CLOSED)
	 */
	public SpotResponse cancelSpot(String spotId) {
		Spot spot = findSpotOrThrow(spotId);
		spot.cancel();
		return toSpotResponse(spot);
	}

	/**
	 * 스팟을 완료 처리합니다. (MATCHED → CLOSED)
	 */
	public SpotResponse completeSpot(String spotId) {
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
	public List<SpotParticipantResponse> getParticipants(String spotId) {
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
	 * 스팟의 일정 목록을 조회합니다.
	 */
	@Transactional(readOnly = true)
	public List<SpotScheduleResponse> getSchedules(String spotId) {
		validateSpotExists(spotId);

		return spotScheduleRepository.findBySpotIdOrderByScheduledAtAsc(spotId)
			.stream()
			.map(SpotScheduleResponse::from)
			.toList();
	}

	/**
	 * 스팟에 일정을 추가합니다.
	 */
	public SpotScheduleResponse addSchedule(String spotId, CreateScheduleRequest request) {
		validateSpotExists(spotId);

		SpotSchedule schedule = SpotSchedule.builder()
			.spotId(spotId)
			.title(request.getTitle())
			.scheduledAt(request.getScheduledAt())
			.build();

		return SpotScheduleResponse.from(spotScheduleRepository.save(schedule));
	}

	// ─────────────────────────────────────────────
	// 투표 (Vote)
	// ─────────────────────────────────────────────

	/**
	 * 스팟의 투표 목록을 선택지 포함하여 조회합니다.
	 */
	@Transactional(readOnly = true)
	public List<SpotVoteResponse> getVotes(String spotId, String currentUserId) {
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
	public SpotVoteResponse createVote(String spotId, CreateVoteRequest request, String currentUserId) {
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
	public SpotVoteResponse castVote(String spotId, Long voteId, CastVoteRequest request, String currentUserId) {
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
		String spotId,
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
	public List<SpotChecklistResponse> getChecklist(String spotId) {
		validateSpotExists(spotId);

		return spotChecklistRepository.findBySpotId(spotId)
			.stream()
			.map(SpotChecklistResponse::from)
			.toList();
	}

	/**
	 * 체크리스트 항목을 추가합니다.
	 */
	public SpotChecklistResponse addChecklistItem(String spotId, CreateChecklistRequest request) {
		validateSpotExists(spotId);

		SpotChecklist item = SpotChecklist.builder()
			.spotId(spotId)
			.content(request.getContent())
			.build();

		return SpotChecklistResponse.from(spotChecklistRepository.save(item));
	}

	/**
	 * 체크리스트 항목의 완료 여부를 토글합니다.
	 * spotId 소속 검증으로 IDOR 를 방지합니다.
	 */
	public SpotChecklistResponse toggleChecklistItem(String spotId, Long itemId) {
		SpotChecklist item = spotChecklistRepository.findById(itemId)
			.filter(i -> i.getSpotId().equals(spotId))
			.orElseThrow(() -> new BusinessException(ErrorCode.CHECKLIST_ITEM_NOT_FOUND));

		item.toggleDone();
		return SpotChecklistResponse.from(item);
	}

	// ─────────────────────────────────────────────
	// 파일 (File)
	// ─────────────────────────────────────────────

	/**
	 * 스팟에 등록된 파일 목록을 최신순으로 조회합니다.
	 */
	@Transactional(readOnly = true)
	public List<SpotFileResponse> getFiles(String spotId) {
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
	public SpotFileResponse uploadFile(String spotId, UploadFileRequest request, String currentUserId) {
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
	public void deleteFile(String spotId, Long fileId) {
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
	public List<SpotNoteResponse> getNotes(String spotId) {
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
	public SpotNoteResponse createNote(String spotId, CreateNoteRequest request, String currentUserId) {
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

	private Spot findSpotOrThrow(String spotId) {
		return spotRepository.findById(spotId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SPOT_NOT_FOUND));
	}

	private void validateSpotExists(String spotId) {
		if (!spotRepository.existsById(spotId)) {
			throw new BusinessException(ErrorCode.SPOT_NOT_FOUND);
		}
	}
}
