package com.conaxgames.poll.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Poll {
    
    private String id;
    private String question;
    private List<String> options;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String createdBy;
    private boolean active;
    private Map<String, Integer> votes;
    private Map<UUID, String> playerVotes;
    
    public Poll(String id, String question, List<String> options, LocalDateTime expiresAt, String createdBy) {
        this.id = id;
        this.question = question;
        this.options = options;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
        this.active = true;
        this.votes = new HashMap<>();
        this.playerVotes = new HashMap<>();
        
        for (String option : options) {
            votes.put(option, 0);
        }
    }

    public boolean vote(UUID playerId, String option) {
        if (!active || isExpired() || !options.contains(option)) {
            return false;
        }
        
        if (playerVotes.containsKey(playerId)) {
            return false;
        }
        
        playerVotes.put(playerId, option);
        votes.put(option, votes.getOrDefault(option, 0) + 1);
        return true;
    }

    public boolean hasVoted(UUID playerId) {
        return playerVotes.containsKey(playerId);
    }

    public String getPlayerVote(UUID playerId) {
        return playerVotes.get(playerId);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public int getTotalVotes() {
        return playerVotes.size();
    }

    public double getOptionPercentage(String option) {
        int total = getTotalVotes();
        if (total == 0) return 0.0;
        return (double) votes.getOrDefault(option, 0) / total;
    }
}
