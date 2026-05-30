package com.errortracker.repository;

import com.errortracker.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, String> {
    List<Project> findByUserIdAndActiveTrue(String userId);
    Optional<Project> findBySdkApiKey(String sdkApiKey);
    boolean existsBySdkApiKey(String sdkApiKey);
}