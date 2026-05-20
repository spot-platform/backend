package backend.notification.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import backend.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, String> {

	Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

	Optional<Notification> findByIdAndUserId(String id, String userId);

	@Modifying
	@Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
	int bulkMarkAllAsRead(@Param("userId") String userId);
}
