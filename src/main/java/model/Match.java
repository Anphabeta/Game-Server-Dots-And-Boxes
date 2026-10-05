package model;

import java.sql.Timestamp;
import java.io.Serializable;

/**
 * Model class đại diện cho bảng Match trong database
 * @author Phạm Tiến Dương
 */
public class Match implements Serializable {
    private static final long serialVersionUID = 1L;

    private int matchId;
    private int player1Id;
    private int player2Id;
    private int player1Score;
    private int player2Score;
    private Integer winnerId; // null nếu hòa
    private String matchStatus; // waiting, playing, finished
    private Timestamp startTime;
    private Timestamp endTime;
    private Timestamp createdAt;

    // Constructors
    public Match() {
        this.player1Score = 0;
        this.player2Score = 0;
        this.matchStatus = "waiting";
    }

    public Match(int player1Id, int player2Id) {
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.player1Score = 0;
        this.player2Score = 0;
        this.matchStatus = "waiting";
    }

    public Match(int matchId, int player1Id, int player2Id, int player1Score,
                 int player2Score, Integer winnerId, String matchStatus,
                 Timestamp startTime, Timestamp endTime) {
        this.matchId = matchId;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.player1Score = player1Score;
        this.player2Score = player2Score;
        this.winnerId = winnerId;
        this.matchStatus = matchStatus;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters and Setters
    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public int getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(int player1Id) {
        this.player1Id = player1Id;
    }

    public int getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(int player2Id) {
        this.player2Id = player2Id;
    }

    public int getPlayer1Score() {
        return player1Score;
    }

    public void setPlayer1Score(int player1Score) {
        this.player1Score = player1Score;
    }

    public int getPlayer2Score() {
        return player2Score;
    }

    public void setPlayer2Score(int player2Score) {
        this.player2Score = player2Score;
    }

    public Integer getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Integer winnerId) {
        this.winnerId = winnerId;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // Helper methods
    public boolean isWaiting() {
        return "waiting".equalsIgnoreCase(matchStatus);
    }

    public boolean isPlaying() {
        return "playing".equalsIgnoreCase(matchStatus);
    }

    public boolean isFinished() {
        return "finished".equalsIgnoreCase(matchStatus);
    }

    public boolean isDraw() {
        return winnerId == null && isFinished();
    }

    public boolean hasPlayer(int userId) {
        return player1Id == userId || player2Id == userId;
    }

    public int getOpponentId(int userId) {
        if (player1Id == userId) return player2Id;
        if (player2Id == userId) return player1Id;
        return -1;
    }

    @Override
    public String toString() {
        return "Match{" +
                "matchId=" + matchId +
                ", player1Id=" + player1Id +
                ", player2Id=" + player2Id +
                ", player1Score=" + player1Score +
                ", player2Score=" + player2Score +
                ", winnerId=" + winnerId +
                ", matchStatus='" + matchStatus + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Match match = (Match) o;
        return matchId == match.matchId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(matchId);
    }
}
