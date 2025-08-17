package com.conaxgames.poll.data;

import com.conaxgames.poll.PollPlugin;
import lombok.Getter;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class PollDataManager {
    
    private final PollPlugin plugin;
    private final Map<String, Poll> activePolls = new ConcurrentHashMap<>();
    private Connection connection;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PollDataManager(PollPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            String dbFile = plugin.getSettings().sqliteFile;
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/" + dbFile);
            createTables();
            loadActivePolls();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS polls (" +
                    "id TEXT PRIMARY KEY," +
                    "question TEXT NOT NULL," +
                    "options TEXT NOT NULL," +
                    "created_at TEXT NOT NULL," +
                    "expires_at TEXT NOT NULL," +
                    "created_by TEXT NOT NULL," +
                    "active INTEGER NOT NULL DEFAULT 1" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS votes (" +
                    "poll_id TEXT NOT NULL," +
                    "player_uuid TEXT NOT NULL," +
                    "option TEXT NOT NULL," +
                    "voted_at TEXT NOT NULL," +
                    "PRIMARY KEY (poll_id, player_uuid)," +
                    "FOREIGN KEY (poll_id) REFERENCES polls(id)" +
                    ")");
        }
    }

    private void loadActivePolls() {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM polls WHERE active = 1 AND expires_at > ?")) {
            stmt.setString(1, LocalDateTime.now().format(DATE_FORMATTER));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Poll poll = loadPollFromResultSet(rs);
                    activePolls.put(poll.getId(), poll);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load active polls: " + e.getMessage());
        }
    }

    private Poll loadPollFromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String question = rs.getString("question");
        String optionsStr = rs.getString("options");
        LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"), DATE_FORMATTER);
        LocalDateTime expiresAt = LocalDateTime.parse(rs.getString("expires_at"), DATE_FORMATTER);
        String createdBy = rs.getString("created_by");
        boolean active = rs.getInt("active") == 1;
        
        List<String> options = Arrays.asList(optionsStr.split("\\|"));
        
        Poll poll = new Poll();
        poll.setId(id);
        poll.setQuestion(question);
        poll.setOptions(options);
        poll.setCreatedAt(createdAt);
        poll.setExpiresAt(expiresAt);
        poll.setCreatedBy(createdBy);
        poll.setActive(active);
        poll.setVotes(new HashMap<>());
        poll.setPlayerVotes(new HashMap<>());
        
        for (String option : options) {
            poll.getVotes().put(option, 0);
        }
        loadVotesForPoll(poll);
        
        return poll;
    }

    private void loadVotesForPoll(Poll poll) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM votes WHERE poll_id = ?")) {
            stmt.setString(1, poll.getId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String playerUuid = rs.getString("player_uuid");
                    String option = rs.getString("option");
                    
                    poll.getPlayerVotes().put(UUID.fromString(playerUuid), option);
                    poll.getVotes().put(option, poll.getVotes().getOrDefault(option, 0) + 1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load votes for poll " + poll.getId() + ": " + e.getMessage());
        }
    }

    public void savePoll(Poll poll) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO polls (id, question, options, created_at, expires_at, created_by, active) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, poll.getId());
            stmt.setString(2, poll.getQuestion());
            stmt.setString(3, String.join("|", poll.getOptions()));
            stmt.setString(4, poll.getCreatedAt().format(DATE_FORMATTER));
            stmt.setString(5, poll.getExpiresAt().format(DATE_FORMATTER));
            stmt.setString(6, poll.getCreatedBy());
            stmt.setInt(7, poll.isActive() ? 1 : 0);
            
            stmt.executeUpdate();
            activePolls.put(poll.getId(), poll);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save poll: " + e.getMessage());
        }
    }

    public void saveVote(String pollId, UUID playerId, String option) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO votes (poll_id, player_uuid, option, voted_at) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, pollId);
            stmt.setString(2, playerId.toString());
            stmt.setString(3, option);
            stmt.setString(4, LocalDateTime.now().format(DATE_FORMATTER));
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save vote: " + e.getMessage());
        }
    }

    public void updatePollStatus(String pollId, boolean active) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE polls SET active = ? WHERE id = ?")) {
            stmt.setInt(1, active ? 1 : 0);
            stmt.setString(2, pollId);
            
            stmt.executeUpdate();
            
            if (!active) {
                activePolls.remove(pollId);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update poll status: " + e.getMessage());
        }
    }

    public void deletePoll(String pollId) {
        try {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM votes WHERE poll_id = ?")) {
                stmt.setString(1, pollId);
                stmt.executeUpdate();
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM polls WHERE id = ?")) {
                stmt.setString(1, pollId);
                stmt.executeUpdate();
            }
            
            activePolls.remove(pollId);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete poll: " + e.getMessage());
        }
    }

    public Collection<Poll> getActivePolls() {
        return activePolls.values();
    }

    public Poll getPoll(String pollId) {
        return activePolls.get(pollId);
    }

    public void cleanupExpiredPolls() {
        List<String> expiredPolls = new ArrayList<>();
        
        for (Poll poll : activePolls.values()) {
            if (poll.isExpired()) {
                expiredPolls.add(poll.getId());
            }
        }
        
        for (String pollId : expiredPolls) {
            updatePollStatus(pollId, false);
        }
    }

    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
            }
        }
    }
}
