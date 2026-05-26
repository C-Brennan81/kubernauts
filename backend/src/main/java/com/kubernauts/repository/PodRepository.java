package com.kubernauts.repository;

import com.kubernauts.model.Pod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PodRepository extends JpaRepository<Pod, Long> {
    List<Pod> findBySessionId(Long sessionId);
    Optional<Pod> findBySessionIdAndName(Long sessionId, String name);
}
