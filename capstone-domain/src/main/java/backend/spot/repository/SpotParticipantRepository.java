package backend.spot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import backend.spot.entity.ParticipantRole;
import backend.spot.entity.ParticipantState;
import backend.spot.entity.SpotParticipant;

public interface SpotParticipantRepository extends JpaRepository<SpotParticipant, Long> {

	List<SpotParticipant> findBySpotId(Long spotId);

	List<SpotParticipant> findByUserId(String userId);

	List<SpotParticipant> findByUserIdOrderByJoinedAtDesc(String userId);

	@Query("SELECT p.spotId FROM SpotParticipant p WHERE p.userId = :userId")
	List<Long> findSpotIdsByUserId(@Param("userId") String userId);

	@Query("SELECT p.spotId FROM SpotParticipant p WHERE p.userId = :userId AND p.state = :state")
	List<Long> findSpotIdsByUserIdAndState(@Param("userId") String userId, @Param("state") ParticipantState state);

	Optional<SpotParticipant> findBySpotIdAndUserId(Long spotId, String userId);

	boolean existsBySpotIdAndUserIdAndState(Long spotId, String userId, ParticipantState state);

	List<SpotParticipant> findBySpotIdAndRole(Long spotId, ParticipantRole role);

	long countBySpotIdAndState(Long spotId, ParticipantState state);
}
