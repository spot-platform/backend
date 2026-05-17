package backend.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, String> {

	List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
}
