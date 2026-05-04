package backend.post.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import backend.post.entity.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

	Optional<Post> findByIdAndDeletedFalse(String id);
}
