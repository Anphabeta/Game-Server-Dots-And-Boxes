package model;

import java.sql.Timestamp;
import java.io.Serializable;

/**
 * Model class đại diện cho bảng User trong database
 * @author Phạm Tiến Dương
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String password;
    private String displayName;
    private int ranking;
    private String status; // online, offline, ingame
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public User() {
        this.ranking = 0;
        this.status = "offline";
    }

    public User(String username, String password, String displayName) {
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.ranking = 0;
        this.status = "offline";
    }

    public User(int userId, String username, String password, String displayName,
                int ranking, String status) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.ranking = ranking;
        this.status = status;
    }

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods
    public boolean isOnline() {
        return "online".equalsIgnoreCase(status);
    }

    public boolean isInGame() {
        return "ingame".equalsIgnoreCase(status);
    }

    public boolean isOffline() {
        return "offline".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                ", ranking=" + ranking +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId == user.userId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(userId);
    }
}
