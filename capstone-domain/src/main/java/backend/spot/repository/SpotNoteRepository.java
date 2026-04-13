package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotNote;

public interface SpotNoteRepository extends JpaRepository<SpotNote, Long> {

	List<SpotNote> findBySpotId(String spotId);

	List<SpotNote> findBySpotIdOrderByCreatedAtDesc(String spotId);
}
