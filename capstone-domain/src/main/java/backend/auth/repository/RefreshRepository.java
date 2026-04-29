package backend.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import backend.auth.entity.RefreshEntity;

public interface RefreshRepository extends JpaRepository<RefreshEntity, String> {

	Optional<RefreshEntity> findByRefresh(String refresh);

	boolean existsByRefresh(String refresh);

	@Modifying
	@Transactional
	@Query("DELETE FROM RefreshEntity r WHERE r.email = :email")
	void deleteByEmail(@Param("email") String email);

	@Modifying
	@Transactional
	@Query("DELETE FROM RefreshEntity r WHERE r.refresh = :refresh")
	void deleteByRefresh(@Param("refresh") String refresh);

	@Modifying
	@Transactional
	@Query("DELETE FROM RefreshEntity r WHERE r.refresh = :refresh")
	int deleteAndCountByRefresh(@Param("refresh") String refresh);

	void deleteByCreatedAtBefore(LocalDateTime threshold);
}
