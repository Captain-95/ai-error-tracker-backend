package com.errortracker.repository;

import com.errortracker.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByUser_IdAndDeletedFalseOrderByCreatedAtDesc(String userId);

    long countByUser_IdAndReadStatusFalseAndDeletedFalse(String userId);

    Optional<Notification> findByIdAndDeletedFalse(String id);

    List<Notification> findByUser_IdAndDeletedFalseAndBookmarked(String userId, Boolean bookmarked);

}