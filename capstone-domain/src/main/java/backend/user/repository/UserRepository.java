package backend.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, String> {

	Optional<UserEntity> findByEmail(String email);

	boolean existsByEmail(String email);
}
