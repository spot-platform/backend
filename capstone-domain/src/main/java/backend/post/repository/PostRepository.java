package backend.post.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import backend.post.entity.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

	Optional<Post> findByIdAndDeletedFalse(String id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Post p WHERE p.id = :id AND p.deleted = false")
	Optional<Post> findByIdAndDeletedFalseWithLock(@Param("id") String id);
}
