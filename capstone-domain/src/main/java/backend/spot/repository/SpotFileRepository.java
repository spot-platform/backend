package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotFile;

public interface SpotFileRepository extends JpaRepository<SpotFile, Long> {

	List<SpotFile> findBySpotId(String spotId);

	List<SpotFile> findBySpotIdOrderByUploadedAtDesc(String spotId);
}
