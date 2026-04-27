package backend.spot.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import backend.spot.dto.UploadFileRequest;
import backend.spot.entity.Spot;
import backend.spot.entity.SpotChecklist;
import backend.spot.entity.SpotFile;
import backend.spot.entity.SpotNote;
import backend.spot.entity.SpotSchedule;
import backend.spot.entity.SpotVote;
import backend.spot.entity.SpotVoteAnswer;
import backend.spot.entity.SpotVoteOption;
import backend.spot.repository.SpotChecklistRepository;
import backend.spot.repository.SpotFileRepository;
import backend.spot.repository.SpotNoteRepository;
import backend.spot.repository.SpotParticipantRepository;
import backend.spot.repository.SpotRepository;
import backend.spot.repository.SpotScheduleRepository;
import backend.spot.repository.SpotVoteAnswerRepository;
import backend.spot.repository.SpotVoteOptionRepository;
import backend.spot.repository.SpotVoteRepository;
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

	// ─────────────────────────────────────────────
	// Spot 기본 CRUD
	// ─────────────────────────────────────────────

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
			.authorId("dummy-user-id")
			.authorNickname("테스트유저")
			.build();

		return SpotResponse.from(spotRepository.save(spot));
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
	 */
	@Transactional(readOnly = true)
	public SpotResponse getSpot(String spotId) {
		return SpotResponse.from(findSpotOrThrow(spotId));
	}

	/**
	 * 스팟을 매칭 상태로 전환합니다. (OPEN → MATCHED)
	 */
	public SpotResponse matchSpot(String spotId) {
		Spot spot = findSpotOrThrow(spotId);
		spot.match();
		return SpotResponse.from(spot);
	}

	/**
	 * 스팟을 취소합니다. (OPEN → CLOSED)
	 */
	public SpotResponse cancelSpot(String spotId) {
		Spot spot = findSpotOrThrow(spotId);
		spot.cancel();
		return SpotResponse.from(spot);
	}

	/**
	 * 스팟을 완료 처리합니다. (MATCHED → CLOSED)
	 */
	public SpotResponse completeSpot(String spotId) {
		Spot spot = findSpotOrThrow(spotId);
		spot.complete();
		return SpotResponse.from(spot);
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

		return spotParticipantRepository.findBySpotId(spotId)
			.stream()
			.map(SpotParticipantResponse::from)
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
	public List<SpotVoteResponse> getVotes(String spotId) {
		validateSpotExists(spotId);

		return spotVoteRepository.findBySpotIdOrderByCreatedAtDesc(spotId)
			.stream()
			.map(vote -> {
				List<SpotVoteOptionResponse> options = spotVoteOptionRepository.findByVoteId(vote.getId())
					.stream()
					.map(SpotVoteOptionResponse::from)
					.toList();
				return SpotVoteResponse.of(vote, options);
			})
			.toList();
	}

	/**
	 * 스팟에 투표를 생성합니다.
	 * TODO: 인증 시스템 도입 후 creatorId 를 실제 로그인 유저 ID로 교체
	 */
	public SpotVoteResponse createVote(String spotId, CreateVoteRequest request) {
		validateSpotExists(spotId);

		SpotVote vote = SpotVote.builder()
			.spotId(spotId)
			.creatorId("dummy-user-id")
			.question(request.getQuestion())
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
			.map(SpotVoteOptionResponse::from)
			.toList();

		return SpotVoteResponse.of(savedVote, optionResponses);
	}

	/**
	 * 투표에 참여(응답)합니다.
	 * - 중복 투표 방지: DB unique constraint (vote_id, user_id) 로 보장
	 * - 선택지 소속 검증: optionId 가 해당 voteId 에 속하는지 확인 (IDOR 방지)
	 * - 원자적 득표 증가: DB UPDATE 쿼리로 race condition 없이 처리
	 * TODO: 인증 시스템 도입 후 userId 를 실제 로그인 유저 ID로 교체
	 */
	public SpotVoteResponse castVote(String spotId, Long voteId, CastVoteRequest request) {
		validateSpotExists(spotId);

		SpotVote vote = spotVoteRepository.findById(voteId)
			.filter(v -> v.getSpotId().equals(spotId))
			.orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));

		spotVoteOptionRepository.findById(request.getOptionId())
			.filter(o -> o.getVoteId().equals(voteId))
			.orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_IN_VOTE));

		String userId = "dummy-user-id";

		SpotVoteAnswer answer = SpotVoteAnswer.builder()
			.voteId(voteId)
			.optionId(request.getOptionId())
			.userId(userId)
			.build();

		spotVoteAnswerRepository.save(answer); // unique constraint 가 중복 투표를 DB 수준에서 차단

		spotVoteOptionRepository.incrementVoteCount(request.getOptionId()); // 원자적 UPDATE

		List<SpotVoteOptionResponse> optionResponses = spotVoteOptionRepository.findByVoteId(voteId)
			.stream()
			.map(SpotVoteOptionResponse::from)
			.toList();

		return SpotVoteResponse.of(vote, optionResponses);
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

		return spotFileRepository.findBySpotIdOrderByUploadedAtDesc(spotId)
			.stream()
			.map(SpotFileResponse::from)
			.toList();
	}

	/**
	 * 스팟에 파일 정보를 등록합니다.
	 * TODO: 인증 시스템 도입 후 uploaderId 를 실제 로그인 유저 ID로 교체
	 */
	public SpotFileResponse uploadFile(String spotId, UploadFileRequest request) {
		validateSpotExists(spotId);

		SpotFile file = SpotFile.builder()
			.spotId(spotId)
			.uploaderId("dummy-user-id")
			.fileName(request.getFileName())
			.fileUrl(request.getFileUrl())
			.build();

		return SpotFileResponse.from(spotFileRepository.save(file));
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

		return spotNoteRepository.findBySpotIdOrderByCreatedAtDesc(spotId)
			.stream()
			.map(SpotNoteResponse::from)
			.toList();
	}

	/**
	 * 스팟에 노트를 작성합니다.
	 * TODO: 인증 시스템 도입 후 authorId 를 실제 로그인 유저 ID로 교체
	 */
	public SpotNoteResponse createNote(String spotId, CreateNoteRequest request) {
		validateSpotExists(spotId);

		SpotNote note = SpotNote.builder()
			.spotId(spotId)
			.authorId("dummy-user-id")
			.content(request.getContent())
			.build();

		return SpotNoteResponse.from(spotNoteRepository.save(note));
	}

	// ─────────────────────────────────────────────
	// 내부 헬퍼
	// ─────────────────────────────────────────────

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
