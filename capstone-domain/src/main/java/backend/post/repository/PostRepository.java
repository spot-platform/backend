package backend.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import backend.post.entity.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {
}
