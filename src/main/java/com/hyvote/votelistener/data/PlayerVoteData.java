package com.hyvote.votelistener.data;
import java.util.HashSet;
import java.util.Set;
/**
 * Model representing a player's vote data including streak tracking.
 *
 * This class stores per-player vote statistics including total votes,
 * current streak, and last vote timestamp for streak calculation.
 */
public class PlayerVoteData {

    /**
     * Player UUID as string for JSON serialization.
     */
    private String uuid;

    /**
     * Player username for logging and display purposes.
     */
    private String username;

    /**
     * Lifetime total vote count for this player.
     */
    private int totalVotes;

    /**
     * Current consecutive daily vote streak count.
     */
    private int currentStreak;

    /**
     * Epoch milliseconds timestamp of the player's last vote.
     */
    private long lastVoteTimestamp;

    /**
     * Sites the player has voted on within the current 24h window.
     */
    private Set<String> voteSites = new HashSet<>();

    /**
     * Timestamp of the first vote in the current 24h window.
     */
    private long firstVoteTimestamp;

    /**
     * Default constructor for Gson deserialization.
     */
    public PlayerVoteData() {
    }

    /**
     * Creates a new PlayerVoteData with all fields initialized.
     *
     * @param uuid Player UUID as string
     * @param username Player username
     * @param totalVotes Lifetime vote count
     * @param currentStreak Current streak count
     * @param lastVoteTimestamp Last vote timestamp in epoch millis
     */
    public PlayerVoteData(String uuid, String username, int totalVotes, int currentStreak, long lastVoteTimestamp) {
        this.uuid = uuid;
        this.username = username;
        this.totalVotes = totalVotes;
        this.currentStreak = currentStreak;
        this.lastVoteTimestamp = lastVoteTimestamp;
    }

    /**
     * Gets the player's UUID.
     *
     * @return Player UUID as string
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Gets the player's username.
     *
     * @return Player username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the player's username.
     *
     * @param username New username value
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the player's total lifetime vote count.
     *
     * @return Total votes
     */
    public int getTotalVotes() {
        return totalVotes;
    }

    /**
     * Sets the player's total vote count.
     *
     * @param totalVotes New total votes value
     */
    public void setTotalVotes(int totalVotes) {
        this.totalVotes = totalVotes;
    }

    /**
     * Gets the player's current consecutive vote streak.
     *
     * @return Current streak count
     */
    public int getCurrentStreak() {
        return currentStreak;
    }

    /**
     * Sets the player's current streak count.
     *
     * @param currentStreak New streak value
     */
    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    /**
     * Gets the timestamp of the player's last vote.
     *
     * @return Last vote timestamp in epoch milliseconds
     */
    public long getLastVoteTimestamp() {
        return lastVoteTimestamp;
    }

    /**
     * Sets the timestamp of the player's last vote.
     *
     * @param lastVoteTimestamp New timestamp in epoch milliseconds
     */
    public void setLastVoteTimestamp(long lastVoteTimestamp) {
        this.lastVoteTimestamp = lastVoteTimestamp;
    }


    /**
     * Adds a voting site and handles 24h reset logic.
     */
    public void addVoteSite(String site) {
        long now = System.currentTimeMillis();
        if (firstVoteTimestamp != 0 && (now - firstVoteTimestamp) > 86400000) {
            voteSites.clear();
            firstVoteTimestamp = now;
        }

        if (voteSites.isEmpty()) {
            firstVoteTimestamp = now;
        }

        voteSites.add(site);
    }

    /**
     * Checks if player has voted on all required sites.
     */
    public boolean hasVotedOnAllSites() {
        return voteSites.contains("HytaleServers")
                && voteSites.contains("HytaleTop100")
                && voteSites.contains("HytaleOnlineServers");
    }

    /**
     * Resets the voting sites tracking.
     */
    public void resetVoteSites() {
        voteSites.clear();
        firstVoteTimestamp = 0;
    }
}
