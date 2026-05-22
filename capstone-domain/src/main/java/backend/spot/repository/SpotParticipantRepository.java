package backend.spot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.ParticipantRole;
import backend.spot.entity.ParticipantState;
import backend.spot.entity.SpotParticipant;

public interface SpotParticipantRepository extends JpaRepository<SpotParticipant, Long> {

	List<SpotParticipant> findBySpotId(Long spotId);

	List<SpotParticipant> findByUserId(String userId);

	List<SpotParticipant> findByUserIdOrderByJoinedAtDesc(String userId);

	Optional<SpotParticipant> findBySpotIdAndUserId(Long spotId, String userId);

	boolean existsBySpotIdAndUserId(Long spotId, String userId);

	List<SpotParticipant> findBySpotIdAndRole(Long spotId, ParticipantRole role);

	long countBySpotIdAndState(Long spotId, ParticipantState state);
}
