package model;

public class LeaderboardEntry {
    private final int rank;
    private final int userId;
    private final String username;
    private final String displayName;
    private final int ranking;
    private final long totalWins;

    public LeaderboardEntry(
            int rank,
            int userId,
            String username,
            String displayName,
            int ranking,
            long totalWins) {
        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.ranking = ranking;
        this.totalWins = totalWins;
    }

    public int getRank() {
        return rank;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRanking() {
        return ranking;
    }

    public long getTotalWins() {
        return totalWins;
    }
}