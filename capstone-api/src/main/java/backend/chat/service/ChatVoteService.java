package backend.chat.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.chat.dto.AddChatVoteOptionRequest;
import backend.chat.dto.CastChatVoteRequest;
import backend.chat.dto.ChatSseEvent;
import backend.chat.dto.ChatVoteOptionResponse;
import backend.chat.dto.ChatVoteResponse;
import backend.chat.dto.CreateChatVoteRequest;
import backend.chat.dto.SubmitChatVoteAnswersRequest;
import backend.chat.entity.ChatVote;
import backend.chat.entity.ChatVoteAnswer;
import backend.chat.entity.ChatVoteOption;
import backend.chat.entity.ChatVoteState;
import backend.chat.repository.ChatVoteAnswerRepository;
import backend.chat.entity.ChatRoomMember;
import backend.chat.repository.ChatRoomMemberRepository;
import backend.chat.repository.ChatVoteOptionRepository;
import backend.chat.repository.ChatVoteRepository;
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅방 투표 서비스.
 *
 * <p>투표 생성·참여·마감·선택지 관리와 SSE 브로드캐스트를 담당한다.
 * 멤버십 검증은 호출자({@link ChatService})가 {@code assertMembership} 으로 처리하고,
 * 본 서비스는 투표 도메인 규칙만 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatVoteService {

	private final ChatVoteRepository chatVoteRepository;
	private final ChatVoteOptionRepository chatVoteOptionRepository;
	private final ChatVoteAnswerRepository chatVoteAnswerRepository;
	private final ChatEventPublisher eventPublisher;
	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final NotificationService notificationService;

	// ─────────────────────────────────────────────
	// 조회
	// ─────────────────────────────────────────────

	@Transactional(readOnly = true)
	public List<ChatVoteResponse> getVotes(Long roomId, String currentUserId) {
		return chatVoteRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId)
			.stream()
			.map(vote -> buildResponse(vote, currentUserId))
			.toList();
	}

	@Transactional(readOnly = true)
	public ChatVoteResponse getVote(Long roomId, Long voteId, String currentUserId) {
		ChatVote vote = findVoteInRoom(voteId, roomId);
		return buildResponse(vote, currentUserId);
	}

	// ─────────────────────────────────────────────
	// 투표 생성
	// ─────────────────────────────────────────────

	public ChatVoteResponse createVote(Long roomId, CreateChatVoteRequest request, String currentUserId) {
		ChatVote vote = chatVoteRepository.save(
			ChatVote.builder()
				.chatRoomId(roomId)
				.creatorId(currentUserId)
				.question(request.getQuestion())
				.multiSelect(request.isMultiSelect())
				.anonymous(request.isAnonymous())
				.closedAt(request.getClosedAt())
				.build()
		);

		List<ChatVoteOption> savedOptions = chatVoteOptionRepository.saveAll(
			request.getOptions().stream()
				.map(content -> ChatVoteOption.builder()
					.voteId(vote.getId())
					.content(content)
					.build())
				.toList()
		);

		ChatVoteResponse response = buildResponse(vote, savedOptions, currentUserId);
		eventPublisher.publishRoom(roomId, ChatSseEvent.voteCreated(response));
		chatRoomMemberRepository.findByChatRoomId(roomId).stream()
			.map(ChatRoomMember::getUserId)
			.filter(uid -> !uid.equals(currentUserId))
			.forEach(uid -> {
				try {
					notificationService.sendAfterCommit(uid, "'" + vote.getQuestion() + "' 투표가 시작됐어요");
				} catch (Exception e) {
					log.warn("[notification] 투표 생성 알림 전송 실패 - roomId={}, userId={}, error={}",
						roomId, uid, e.getMessage());
				}
			});
		return response;
	}

	// ─────────────────────────────────────────────
	// 투표 참여 (단건 토글)
	// ─────────────────────────────────────────────

	/**
	 * 토글 시맨틱: 같은 옵션 재캐스트 = 투표 해제.
	 * 단일선택 시 다른 옵션 선택 중이면 기존 표를 교체.
	 */
	public ChatVoteResponse castVote(Long roomId, Long voteId, CastChatVoteRequest request, String currentUserId) {
		ChatVote vote = findActiveVoteInRoom(voteId, roomId);

		Long optionId = request.getOptionId();
		ChatVoteOption option = chatVoteOptionRepository.findById(optionId)
			.filter(o -> o.getVoteId().equals(voteId))
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_VOTE_OPTION_NOT_IN_VOTE));

		List<ChatVoteAnswer> myAnswers = chatVoteAnswerRepository.findByVoteIdAndUserId(voteId, currentUserId);
		Optional<ChatVoteAnswer> existingOnSame = myAnswers.stream()
			.filter(a -> a.getOptionId().equals(optionId))
			.findFirst();

		if (existingOnSame.isPresent()) {
			// 토글 OFF
			chatVoteAnswerRepository.delete(existingOnSame.get());
			chatVoteAnswerRepository.flush();
			chatVoteOptionRepository.decrementVoteCount(optionId);
		} else {
			// 단일선택: 기존 표 교체
			if (!vote.isMultiSelect() && !myAnswers.isEmpty()) {
				chatVoteAnswerRepository.deleteByVoteIdAndUserId(voteId, currentUserId);
				chatVoteAnswerRepository.flush();
				myAnswers.forEach(prev -> chatVoteOptionRepository.decrementVoteCount(prev.getOptionId()));
			}
			chatVoteAnswerRepository.save(
				ChatVoteAnswer.builder()
					.voteId(voteId)
					.optionId(optionId)
					.userId(currentUserId)
					.build()
			);
			chatVoteOptionRepository.incrementVoteCount(optionId);
		}

		ChatVoteResponse response = buildResponse(vote, currentUserId);
		eventPublisher.publishRoom(roomId, ChatSseEvent.voteUpdated(response));
		return response;
	}

	// ─────────────────────────────────────────────
	// 투표 일괄 제출 (최종 확정 상태)
	// ─────────────────────────────────────────────

	/**
	 * 요청의 optionIds 가 최종 확정 상태. 서버가 현재 답변과 diff 하여 원자적으로 적용.
	 * 빈 배열 = 전체 취소.
	 */
	public ChatVoteResponse submitAnswers(Long roomId, Long voteId, SubmitChatVoteAnswersRequest request,
		String currentUserId) {
		ChatVote vote = findActiveVoteInRoom(voteId, roomId);

		Set<Long> desiredOptionIds = new HashSet<>(request.getOptionIds());

		// IDOR 방지: 모든 optionId가 해당 voteId 소속인지 검증
		if (!desiredOptionIds.isEmpty()) {
			List<Long> validOptionIds = chatVoteOptionRepository.findByVoteId(voteId)
				.stream()
				.map(ChatVoteOption::getId)
				.toList();
			Set<Long> validSet = new HashSet<>(validOptionIds);
			if (!validSet.containsAll(desiredOptionIds)) {
				throw new BusinessException(ErrorCode.CHAT_VOTE_OPTION_NOT_IN_VOTE);
			}
		}

		// 단일선택: 최대 1개
		if (!vote.isMultiSelect() && desiredOptionIds.size() > 1) {
			throw new BusinessException(ErrorCode.CHAT_VOTE_SINGLE_SELECT_LIMIT);
		}

		List<ChatVoteAnswer> myAnswers = chatVoteAnswerRepository.findByVoteIdAndUserId(voteId, currentUserId);
		Set<Long> currentOptionIds = myAnswers.stream().map(ChatVoteAnswer::getOptionId).collect(Collectors.toSet());

		Set<Long> toAdd = new HashSet<>(desiredOptionIds);
		toAdd.removeAll(currentOptionIds);

		Set<Long> toRemove = new HashSet<>(currentOptionIds);
		toRemove.removeAll(desiredOptionIds);

		// 삭제 먼저 (unique constraint 충돌 방지)
		for (ChatVoteAnswer ans : myAnswers) {
			if (toRemove.contains(ans.getOptionId())) {
				chatVoteAnswerRepository.delete(ans);
				chatVoteOptionRepository.decrementVoteCount(ans.getOptionId());
			}
		}
		chatVoteAnswerRepository.flush();

		for (Long addId : toAdd) {
			chatVoteAnswerRepository.save(
				ChatVoteAnswer.builder()
					.voteId(voteId)
					.optionId(addId)
					.userId(currentUserId)
					.build()
			);
			chatVoteOptionRepository.incrementVoteCount(addId);
		}

		ChatVoteResponse response = buildResponse(vote, currentUserId);
		eventPublisher.publishRoom(roomId, ChatSseEvent.voteUpdated(response));
		return response;
	}

	// ─────────────────────────────────────────────
	// 투표 마감 (창작자 전용)
	// ─────────────────────────────────────────────

	public ChatVoteResponse closeVote(Long roomId, Long voteId, String currentUserId) {
		ChatVote vote = findActiveVoteInRoom(voteId, roomId);
		if (!vote.getCreatorId().equals(currentUserId)) {
			throw new BusinessException(ErrorCode.CHAT_VOTE_NOT_CREATOR);
		}
		vote.close();
		ChatVoteResponse response = buildResponse(vote, currentUserId);
		eventPublisher.publishRoom(roomId, ChatSseEvent.voteClosed(response));
		chatRoomMemberRepository.findByChatRoomId(roomId).stream()
			.map(ChatRoomMember::getUserId)
			.filter(uid -> !uid.equals(currentUserId))
			.forEach(uid -> {
				try {
					notificationService.sendAfterCommit(uid, "'" + vote.getQuestion() + "' 투표가 마감됐어요");
				} catch (Exception e) {
					log.warn("[notification] 투표 마감 알림 전송 실패 - roomId={}, voteId={}, userId={}, error={}",
						roomId, voteId, uid, e.getMessage());
				}
			});
		return response;
	}

	// ─────────────────────────────────────────────
	// 선택지 추가 (창작자 전용)
	// ─────────────────────────────────────────────

	public ChatVoteResponse addOption(Long roomId, Long voteId, AddChatVoteOptionRequest request,
		String currentUserId) {
		ChatVote vote = findActiveVoteInRoom(voteId, roomId);
		if (!vote.getCreatorId().equals(currentUserId)) {
			throw new BusinessException(ErrorCode.CHAT_VOTE_NOT_CREATOR);
		}
		chatVoteOptionRepository.save(
			ChatVoteOption.builder()
				.voteId(voteId)
				.content(request.getContent())
				.build()
		);
		ChatVoteResponse response = buildResponse(vote, currentUserId);
		eventPublisher.publishRoom(roomId, ChatSseEvent.voteUpdated(response));
		return response;
	}

	// ─────────────────────────────────────────────
	// 선택지 삭제 (창작자 전용)
	// ─────────────────────────────────────────────

	public ChatVoteResponse removeOption(Long roomId, Long voteId, Long optionId, String currentUserId) {
		ChatVote vote = findActiveVoteInRoom(voteId, roomId);
		if (!vote.getCreatorId().equals(currentUserId)) {
			throw new BusinessException(ErrorCode.CHAT_VOTE_NOT_CREATOR);
		}
		ChatVoteOption option = chatVoteOptionRepository.findById(optionId)
			.filter(o -> o.getVoteId().equals(voteId))
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_VOTE_OPTION_NOT_IN_VOTE));

		// 해당 선택지에 투표한 답변 먼저 삭제
		chatVoteAnswerRepository.deleteByOptionId(option.getId());
		chatVoteAnswerRepository.flush();
		chatVoteOptionRepository.delete(option);

		ChatVoteResponse response = buildResponse(vote, currentUserId);
		eventPublisher.publishRoom(roomId, ChatSseEvent.voteUpdated(response));
		return response;
	}

	// ─────────────────────────────────────────────
	// 스케줄러: 30분 전 알림 + 자동 마감
	// ─────────────────────────────────────────────

	/**
	 * 매 5분마다 실행.
	 * - closedAt이 지금으로부터 25~35분 사이이고 아직 알림 미발송인 투표 → VOTE_CLOSING_SOON 브로드캐스트
	 * - closedAt이 이미 지난 ACTIVE 투표 → 자동 마감 처리 + VOTE_CLOSED 브로드캐스트
	 */
	@Scheduled(fixedRate = 5 * 60 * 1000)
	public void processScheduledVotes() {
		LocalDateTime now = LocalDateTime.now();

		// 30분 전 알림
		List<ChatVote> closingSoon = chatVoteRepository.findByStateAndNotifySentFalseAndClosedAtBetween(
			ChatVoteState.ACTIVE, now.plusMinutes(25), now.plusMinutes(35)
		);
		for (ChatVote vote : closingSoon) {
			try {
				eventPublisher.publishRoom(vote.getChatRoomId(),
					ChatSseEvent.voteClosingSoon(vote.getChatRoomId(), vote.getId(), vote.getQuestion()));
				vote.markNotifySent();
			} catch (Exception e) {
				log.error("[VoteScheduler] closing-soon notify failed voteId={}", vote.getId(), e);
			}
		}

		// 자동 마감
		List<ChatVote> toClose = chatVoteRepository.findByStateAndClosedAtBefore(ChatVoteState.ACTIVE, now);
		for (ChatVote vote : toClose) {
			try {
				vote.close();
				ChatVoteResponse response = buildResponse(vote, null);
				eventPublisher.publishRoom(vote.getChatRoomId(), ChatSseEvent.voteClosed(response));
			} catch (Exception e) {
				log.error("[VoteScheduler] auto-close failed voteId={}", vote.getId(), e);
			}
		}
	}

	// ─────────────────────────────────────────────
	// 내부 헬퍼
	// ─────────────────────────────────────────────

	private ChatVote findVoteInRoom(Long voteId, Long roomId) {
		ChatVote vote = chatVoteRepository.findById(voteId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_VOTE_NOT_FOUND));
		if (!vote.getChatRoomId().equals(roomId)) {
			throw new BusinessException(ErrorCode.CHAT_VOTE_NOT_FOUND);
		}
		return vote;
	}

	private ChatVote findActiveVoteInRoom(Long voteId, Long roomId) {
		ChatVote vote = findVoteInRoom(voteId, roomId);
		if (vote.getState() != ChatVoteState.ACTIVE) {
			throw new BusinessException(ErrorCode.CHAT_VOTE_NOT_ACTIVE);
		}
		return vote;
	}

	private ChatVoteResponse buildResponse(ChatVote vote, String currentUserId) {
		List<ChatVoteOption> options = chatVoteOptionRepository.findByVoteId(vote.getId());
		return buildResponse(vote, options, currentUserId);
	}

	private ChatVoteResponse buildResponse(ChatVote vote, List<ChatVoteOption> options, String currentUserId) {
		List<ChatVoteAnswer> allAnswers = chatVoteAnswerRepository.findByVoteId(vote.getId());

		// optionId → voterIds 매핑
		Map<Long, List<String>> votersByOption = allAnswers.stream()
			.collect(Collectors.groupingBy(
				ChatVoteAnswer::getOptionId,
				Collectors.mapping(ChatVoteAnswer::getUserId, Collectors.toList())
			));

		List<ChatVoteOptionResponse> optionResponses = options.stream()
			.map(opt -> ChatVoteOptionResponse.of(
				opt,
				votersByOption.getOrDefault(opt.getId(), List.of()),
				vote.isAnonymous()
			))
			.toList();

		// 나의 투표 선택지
		List<Long> myVotedOptionIds = currentUserId == null ? List.of()
			: allAnswers.stream()
				.filter(a -> a.getUserId().equals(currentUserId))
				.map(ChatVoteAnswer::getOptionId)
				.toList();

		// 총 투표 참여자 수 (중복 없이 유저 단위)
		long totalVoterCount = allAnswers.stream()
			.map(ChatVoteAnswer::getUserId)
			.distinct()
			.count();

		return ChatVoteResponse.of(vote, optionResponses, myVotedOptionIds, (int) totalVoterCount);
	}
}
