package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotSettlementLineItem;

public interface SpotSettlementLineItemRepository extends JpaRepository<SpotSettlementLineItem, Long> {

	List<SpotSettlementLineItem> findBySettlementId(Long settlementId);
}
