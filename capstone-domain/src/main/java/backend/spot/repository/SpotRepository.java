package backend.spot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import backend.spot.entity.Spot;

@Repository
public interface SpotRepository extends JpaRepository<Spot, String> {
}
