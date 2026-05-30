package com.errortracker.repository;

import com.errortracker.model.ProjectNotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectNotificationRecipientRepository extends JpaRepository<ProjectNotificationRecipient, String> {

    List<ProjectNotificationRecipient> findByProjectId(String projectId);

    void deleteByProjectId(String projectId);
}