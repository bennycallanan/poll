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
    private int nextPollId = 1;

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
                    "id INTEGER PRIMARY KEY," +
                    "question TEXT NOT NULL," +
                    "options TEXT NOT NULL," +
                    "created_at TEXT NOT NULL," +
                    "expires_at TEXT NOT NULL," +
                    "created_by TEXT NOT NULL," +
                    "active INTEGER NOT NULL DEFAULT 1" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS votes (" +
                    "poll_id INTEGER NOT NULL," +
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
                "SELECT * FROM polls WHERE active = 1")) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                int maxId = 0;
                while (rs.next()) {
                    Poll poll = loadPollFromResultSet(rs);
                    
                    if (poll.isExpired()) {
                        plugin.getLogger().info("Found expired poll " + poll.getId() + " with active=1, marking as inactive");
                        updatePollStatus(poll.getId(), false);
                    } else {
                        activePolls.put(poll.getId(), poll);
                    }
                    
                    maxId = Math.max(maxId, Integer.parseInt(poll.getId()));
                }
                nextPollId = maxId + 1;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load active polls: " + e.getMessage());
        }
    }

    private Poll loadPollFromResultSet(ResultSet rs) throws SQLException {
        String id = String.valueOf(rs.getInt("id"));
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
        executeQuery("SELECT * FROM votes WHERE poll_id = ?", 
            stmt -> stmt.setString(1, poll.getId()),
            rs -> {
                while (rs.next()) {
                    String playerUuid = rs.getString("player_uuid");
                    String option = rs.getString("option");
                    
                    poll.getPlayerVotes().put(UUID.fromString(playerUuid), option);
                    poll.getVotes().put(option, poll.getVotes().getOrDefault(option, 0) + 1);
                }
            },
            "Failed to load votes for poll " + poll.getId()
        );
    }

    public void savePoll(Poll poll) {
        executeUpdate("INSERT INTO polls (id, question, options, created_at, expires_at, created_by, active) VALUES (?, ?, ?, ?, ?, ?, ?)",
            stmt -> {
                stmt.setInt(1, Integer.parseInt(poll.getId()));
                stmt.setString(2, poll.getQuestion());
                stmt.setString(3, String.join("|", poll.getOptions()));
                stmt.setString(4, poll.getCreatedAt().format(DATE_FORMATTER));
                stmt.setString(5, poll.getExpiresAt().format(DATE_FORMATTER));
                stmt.setString(6, poll.getCreatedBy());
                stmt.setInt(7, poll.isActive() ? 1 : 0);
            },
            "Failed to save poll"
        );
        activePolls.put(poll.getId(), poll);
    }

    public void saveVote(String pollId, UUID playerId, String option) {
        executeUpdate("INSERT INTO votes (poll_id, player_uuid, option, voted_at) VALUES (?, ?, ?, ?)",
            stmt -> {
                stmt.setInt(1, Integer.parseInt(pollId));
                stmt.setString(2, playerId.toString());
                stmt.setString(3, option);
                stmt.setString(4, LocalDateTime.now().format(DATE_FORMATTER));
            },
            "Failed to save vote"
        );
    }

    public void updatePollStatus(String pollId, boolean active) {
        executeUpdate("UPDATE polls SET active = ? WHERE id = ?",
            stmt -> {
                stmt.setInt(1, active ? 1 : 0);
                stmt.setInt(2, Integer.parseInt(pollId));
            },
            "Failed to update poll status"
        );
        
        if (!active) {
            activePolls.remove(pollId);
        }
    }

    public void deletePoll(String pollId) {
        executeUpdate("DELETE FROM votes WHERE poll_id = ?",
            stmt -> stmt.setInt(1, Integer.parseInt(pollId)),
            "Failed to delete votes for poll"
        );
        
        executeUpdate("DELETE FROM polls WHERE id = ?",
            stmt -> stmt.setInt(1, Integer.parseInt(pollId)),
            "Failed to delete poll"
        );
        
        activePolls.remove(pollId);
    }

    public Collection<Poll> getActivePolls() {
        return activePolls.values();
    }

    public Poll getPoll(String pollId) {
        return activePolls.get(pollId);
    }

    public void cleanupExpiredPolls() {
        executeQuery("SELECT id FROM polls WHERE active = 1 AND expires_at <= ?",
            stmt -> stmt.setString(1, LocalDateTime.now().format(DATE_FORMATTER)),
            rs -> {
                while (rs.next()) {
                    String pollId = String.valueOf(rs.getInt("id"));
                    plugin.getLogger().info("Cleaning up expired poll " + pollId);
                    updatePollStatus(pollId, false);
                }
            },
            "Failed to cleanup expired polls"
        );
    }

    public String getNextPollId() {
        return String.valueOf(nextPollId++);
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
    
    private void executeQuery(String sql, SQLConsumer<PreparedStatement> parameterSetter, 
                             SQLConsumer<ResultSet> resultProcessor, String errorMessage) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            parameterSetter.accept(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                resultProcessor.accept(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(errorMessage + ": " + e.getMessage());
        }
    }
    
    private void executeUpdate(String sql, SQLConsumer<PreparedStatement> parameterSetter, String errorMessage) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            parameterSetter.accept(stmt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe(errorMessage + ": " + e.getMessage());
        }
    }
    
    @FunctionalInterface
    private interface SQLConsumer<T> {
        void accept(T t) throws SQLException;
    }
}
