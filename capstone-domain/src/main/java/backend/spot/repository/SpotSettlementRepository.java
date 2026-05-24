package backend.spot.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotSettlement;
import backend.spot.entity.WorkflowApprovalStatus;

public interface SpotSettlementRepository extends JpaRepository<SpotSettlement, Long> {

	Optional<SpotSettlement> findFirstBySpotIdAndStatusOrderByCreatedAtDesc(
		Long spotId, WorkflowApprovalStatus status);
}
