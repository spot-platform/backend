package backend.spot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.ParticipantRole;
import backend.spot.entity.SpotParticipant;

public interface SpotParticipantRepository extends JpaRepository<SpotParticipant, Long> {

	List<SpotParticipant> findBySpotId(String spotId);

	List<SpotParticipant> findByUserId(String userId);

	Optional<SpotParticipant> findBySpotIdAndUserId(String spotId, String userId);

	boolean existsBySpotIdAndUserId(String spotId, String userId);

	List<SpotParticipant> findBySpotIdAndRole(String spotId, ParticipantRole role);
}
