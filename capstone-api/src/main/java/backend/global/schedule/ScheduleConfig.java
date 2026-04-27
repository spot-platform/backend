package backend.global.schedule;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import backend.auth.repository.RefreshRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleConfig {

	private final RefreshRepository refreshRepository;

	@Value("${jwt.refresh-cleanup-days:8}")
	private long refreshCleanupDays;

	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	@Transactional
	public void deleteExpiredRefreshTokens() {
		LocalDateTime threshold = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(refreshCleanupDays);
		refreshRepository.deleteByCreatedAtBefore(threshold);
		log.info("Expired refresh tokens deleted before {}", threshold);
	}
}
