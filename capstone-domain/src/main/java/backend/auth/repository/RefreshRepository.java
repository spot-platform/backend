package backend.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.auth.entity.RefreshEntity;

public interface RefreshRepository extends JpaRepository<RefreshEntity, String> {

	Optional<RefreshEntity> findByRefresh(String refresh);

	boolean existsByRefresh(String refresh);

	void deleteByEmail(String email);

	void deleteByRefresh(String refresh);

	void deleteByCreatedAtBefore(LocalDateTime threshold);
}
