package model;

import java.sql.Timestamp;
import java.io.Serializable;

/**
 * Model class đại diện cho bảng UserHistoryMatch trong database
 * @author Phạm Tiến Dương
 */
public class UserHistoryMatch implements Serializable {
    private static final long serialVersionUID = 1L;

    private int historyId;
    private int userId;
    private int matchId;
    private String result; // win, lose, draw
    private Timestamp time;

    // Constructors
    public UserHistoryMatch() {
    }

    public UserHistoryMatch(int userId, int matchId, String result) {
        this.userId = userId;
        this.matchId = matchId;
        this.result = result;
    }

    public UserHistoryMatch(int historyId, int userId, int matchId, String result, Timestamp time) {
        this.historyId = historyId;
        this.userId = userId;
        this.matchId = matchId;
        this.result = result;
        this.time = time;
    }

    // Getters and Setters
    public int getHistoryId() {
        return historyId;
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    // Helper methods
    public boolean isWin() {
        return "win".equalsIgnoreCase(result);
    }

    public boolean isLose() {
        return "lose".equalsIgnoreCase(result);
    }

    public boolean isDraw() {
        return "draw".equalsIgnoreCase(result);
    }

    @Override
    public String toString() {
        return "UserHistoryMatch{" +
                "historyId=" + historyId +
                ", userId=" + userId +
                ", matchId=" + matchId +
                ", result='" + result + '\'' +
                ", time=" + time +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserHistoryMatch that = (UserHistoryMatch) o;
        return historyId == that.historyId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(historyId);
    }
}
