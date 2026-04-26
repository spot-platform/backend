package backend.global.schedule;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import backend.auth.repository.RefreshRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduleConfig {

	private final RefreshRepository refreshRepository;

	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	@Transactional
	public void deleteExpiredRefreshTokens() {
		LocalDateTime threshold = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(8);
		refreshRepository.deleteByCreatedAtBefore(threshold);
		log.info("Expired refresh tokens deleted before {}", threshold);
	}
}
