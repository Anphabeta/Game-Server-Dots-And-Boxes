package model;

import java.io.Serializable;

/**
 * Model class mở rộng chứa thông tin chi tiết của Match
 * Bao gồm thông tin về người chơi (username, displayName)
 * @author Phạm Tiến Dương
 */
public class MatchEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    private int matchId;
    private int player1Id;
    private String player1Username;
    private String player1DisplayName;
    private int player1Score;

    private int player2Id;
    private String player2Username;
    private String player2DisplayName;
    private int player2Score;

    private Integer winnerId;
    private String winnerName;
    private String matchStatus;
    private java.sql.Timestamp startTime;
    private java.sql.Timestamp endTime;
    private int durationMinutes;

    // Constructors
    public MatchEntity() {
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

    public String getPlayer1Username() {
        return player1Username;
    }

    public void setPlayer1Username(String player1Username) {
        this.player1Username = player1Username;
    }

    public String getPlayer1DisplayName() {
        return player1DisplayName;
    }

    public void setPlayer1DisplayName(String player1DisplayName) {
        this.player1DisplayName = player1DisplayName;
    }

    public int getPlayer1Score() {
        return player1Score;
    }

    public void setPlayer1Score(int player1Score) {
        this.player1Score = player1Score;
    }

    public int getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(int player2Id) {
        this.player2Id = player2Id;
    }

    public String getPlayer2Username() {
        return player2Username;
    }

    public void setPlayer2Username(String player2Username) {
        this.player2Username = player2Username;
    }

    public String getPlayer2DisplayName() {
        return player2DisplayName;
    }

    public void setPlayer2DisplayName(String player2DisplayName) {
        this.player2DisplayName = player2DisplayName;
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

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public java.sql.Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(java.sql.Timestamp startTime) {
        this.startTime = startTime;
    }

    public java.sql.Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(java.sql.Timestamp endTime) {
        this.endTime = endTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    // Helper methods
    public boolean isDraw() {
        return winnerId == null;
    }

    public boolean isPlayer1Winner() {
        return winnerId != null && winnerId == player1Id;
    }

    public boolean isPlayer2Winner() {
        return winnerId != null && winnerId == player2Id;
    }

    @Override
    public String toString() {
        return "MatchEntity{" +
                "matchId=" + matchId +
                ", player1=" + player1DisplayName + "(" + player1Score + ")" +
                ", player2=" + player2DisplayName + "(" + player2Score + ")" +
                ", winner=" + winnerName +
                ", status='" + matchStatus + '\'' +
                '}';
    }
}
