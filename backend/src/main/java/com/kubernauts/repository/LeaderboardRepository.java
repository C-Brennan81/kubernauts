package com.kubernauts.repository;

import com.kubernauts.model.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {

    @Query("SELECT e FROM LeaderboardEntry e ORDER BY e.score DESC LIMIT 10")
    List<LeaderboardEntry> findTop10();
}
