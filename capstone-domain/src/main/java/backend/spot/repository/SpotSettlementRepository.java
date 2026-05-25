package backend.spot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotSettlement;

public interface SpotSettlementRepository extends JpaRepository<SpotSettlement, Long> {

	Optional<SpotSettlement> findFirstBySpotIdOrderByCreatedAtDescIdDesc(Long spotId);
}
