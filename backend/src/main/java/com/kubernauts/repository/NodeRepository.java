package com.kubernauts.repository;

import com.kubernauts.model.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NodeRepository extends JpaRepository<Node, Long> {
    List<Node> findBySessionId(Long sessionId);
}
