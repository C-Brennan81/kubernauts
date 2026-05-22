package com.kubernauts.service;

import com.kubernauts.model.LeaderboardEntry;
import com.kubernauts.repository.LeaderboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardRepository repo;

    public List<LeaderboardEntry> getTop10() {
        return repo.findTop10();
    }

    /** Persist score and return the player's rank (1-based). */
    public int recordScore(String playerName, int score) {
        repo.save(new LeaderboardEntry(playerName, score));
        List<LeaderboardEntry> top = repo.findTop10();
        for (int i = 0; i < top.size(); i++) {
            if (top.get(i).getPlayerName().equals(playerName)
                    && top.get(i).getScore() == score) {
                return i + 1;
            }
        }
        return top.size() + 1; // outside top 10
    }
}
