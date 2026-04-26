package backend.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import backend.auth.entity.RefreshEntity;

public interface RefreshRepository extends JpaRepository<RefreshEntity, String> {

	Optional<RefreshEntity> findByRefresh(String refresh);

	boolean existsByRefresh(String refresh);

	void deleteByEmail(String email);

	void deleteByRefresh(String refresh);

	@Modifying
	@Query("DELETE FROM RefreshEntity r WHERE r.refresh = :refresh")
	int deleteAndCountByRefresh(@Param("refresh") String refresh);

	void deleteByCreatedAtBefore(LocalDateTime threshold);
}
