package dao;

import model.Match;
import model.MatchEntity;
import model.UserHistoryMatch;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho bảng Match và UserHistoryMatch
 * @author Phạm Tiến Dương
 */
public class MatchDAO {

    private DBConnection dbConnection;

    public MatchDAO() {
        this.dbConnection = DBConnection.getInstance();
    }

    /**
     * Tạo match mới
     * @return matchId nếu thành công, -1 nếu thất bại
     */
    public int createMatch(Match match) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "INSERT INTO `Match` (player1Id, player2Id, player1Score, player2Score, " +
                        "matchStatus, startTime) VALUES (?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, match.getPlayer1Id());
            ps.setInt(2, match.getPlayer2Id());
            ps.setInt(3, match.getPlayer1Score());
            ps.setInt(4, match.getPlayer2Score());
            ps.setString(5, match.getMatchStatus());
            ps.setTimestamp(6, match.getStartTime());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int matchId = generatedKeys.getInt(1);
                    match.setMatchId(matchId);
                    return matchId;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error in createMatch: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, ps, conn);
        }

        return -1;
    }

    /**
     * Tìm match theo matchId
     */
    public Match findById(int matchId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT * FROM `Match` WHERE matchId = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, matchId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return extractMatchFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error in findById: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return null;
    }

    /**
     * Cập nhật match
     */
    public boolean updateMatch(Match match) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "UPDATE `Match` SET player1Score = ?, player2Score = ?, " +
                        "winnerId = ?, matchStatus = ?, endTime = ? WHERE matchId = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, match.getPlayer1Score());
            ps.setInt(2, match.getPlayer2Score());

            if (match.getWinnerId() != null) {
                ps.setInt(3, match.getWinnerId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setString(4, match.getMatchStatus());
            ps.setTimestamp(5, match.getEndTime());
            ps.setInt(6, match.getMatchId());

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error in updateMatch: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, ps, conn);
        }

        return false;
    }

    /**
     * Cập nhật score của match
     */
    public boolean updateMatchScore(int matchId, int player1Score, int player2Score) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "UPDATE `Match` SET player1Score = ?, player2Score = ? WHERE matchId = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, player1Score);
            ps.setInt(2, player2Score);
            ps.setInt(3, matchId);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error in updateMatchScore: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, ps, conn);
        }

        return false;
    }

    /**
     * Bắt đầu match (cập nhật status và startTime)
     */
    public boolean startMatch(int matchId) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "UPDATE `Match` SET matchStatus = 'playing', startTime = CURRENT_TIMESTAMP " +
                        "WHERE matchId = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, matchId);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error in startMatch: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, ps, conn);
        }

        return false;
    }

    /**
     * Kết thúc match và tạo history (sử dụng stored procedure)
     */
    public boolean finishMatch(int matchId, int player1Score, int player2Score) {
        Connection conn = null;
        CallableStatement cs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "{CALL FinishMatch(?, ?, ?)}";
            cs = conn.prepareCall(sql);
            cs.setInt(1, matchId);
            cs.setInt(2, player1Score);
            cs.setInt(3, player2Score);

            cs.execute();
            return true;

        } catch (SQLException e) {
            System.err.println("Error in finishMatch: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, cs, conn);
        }

        return false;
    }

    /**
     * Lấy danh sách match theo status
     */
    public List<Match> getMatchesByStatus(String status) {
        List<Match> matches = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT * FROM `Match` WHERE matchStatus = ? ORDER BY createdAt DESC";
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            rs = ps.executeQuery();

            while (rs.next()) {
                matches.add(extractMatchFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error in getMatchesByStatus: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return matches;
    }

    /**
     * Lấy danh sách match của một user
     */
    public List<Match> getMatchesByUserId(int userId) {
        List<Match> matches = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT * FROM `Match` WHERE player1Id = ? OR player2Id = ? " +
                        "ORDER BY createdAt DESC";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            rs = ps.executeQuery();

            while (rs.next()) {
                matches.add(extractMatchFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error in getMatchesByUserId: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return matches;
    }

    /**
     * Lấy chi tiết match với thông tin người chơi (từ view MatchDetails)
     */
    public MatchEntity getMatchEntityById(int matchId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT * FROM MatchDetails WHERE matchId = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, matchId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return extractMatchEntityFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error in getMatchEntityById: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return null;
    }

    /**
     * Lấy danh sách MatchEntity theo status
     */
    public List<MatchEntity> getMatchEntitiesByStatus(String status) {
        List<MatchEntity> matches = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT * FROM MatchDetails WHERE matchStatus = ? ORDER BY matchId DESC";
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            rs = ps.executeQuery();

            while (rs.next()) {
                matches.add(extractMatchEntityFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error in getMatchEntitiesByStatus: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return matches;
    }

    /**
     * Lấy lịch sử match của user
     */
    public List<UserHistoryMatch> getHistoryByUserId(int userId) {
        List<UserHistoryMatch> history = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT * FROM UserHistoryMatch WHERE userId = ? ORDER BY time DESC";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();

            while (rs.next()) {
                history.add(extractHistoryFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error in getHistoryByUserId: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return history;
    }

    /**
     * Lấy lịch sử match của user với limit
     */
    public List<UserHistoryMatch> getHistoryByUserId(int userId, int limit) {
        List<UserHistoryMatch> history = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT * FROM UserHistoryMatch WHERE userId = ? ORDER BY time DESC LIMIT ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            rs = ps.executeQuery();

            while (rs.next()) {
                history.add(extractHistoryFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error in getHistoryByUserId with limit: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return history;
    }

    /**
     * Thêm một record vào UserHistoryMatch
     */
    public boolean addHistory(UserHistoryMatch history) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "INSERT INTO UserHistoryMatch (userId, matchId, result) VALUES (?, ?, ?)";
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, history.getUserId());
            ps.setInt(2, history.getMatchId());
            ps.setString(3, history.getResult());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    history.setHistoryId(generatedKeys.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error in addHistory: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, ps, conn);
        }

        return false;
    }

    /**
     * Đếm số trận thắng của user
     */
    public int countWinsByUserId(int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM UserHistoryMatch WHERE userId = ? AND result = 'win'";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error in countWinsByUserId: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return 0;
    }

    /**
     * Đếm số trận thua của user
     */
    public int countLosesByUserId(int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM UserHistoryMatch WHERE userId = ? AND result = 'lose'";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error in countLosesByUserId: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return 0;
    }

    /**
     * Đếm số trận hòa của user
     */
    public int countDrawsByUserId(int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM UserHistoryMatch WHERE userId = ? AND result = 'draw'";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error in countDrawsByUserId: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return 0;
    }

    /**
     * Đếm tổng số trận đấu của user
     */
    public int countTotalMatchesByUserId(int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM UserHistoryMatch WHERE userId = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error in countTotalMatchesByUserId: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return 0;
    }

    /**
     * Xóa match
     */
    public boolean deleteMatch(int matchId) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "DELETE FROM `Match` WHERE matchId = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, matchId);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error in deleteMatch: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, ps, conn);
        }

        return false;
    }

    /**
     * Helper method: Extract Match từ ResultSet
     */
    private Match extractMatchFromResultSet(ResultSet rs) throws SQLException {
        Match match = new Match();
        match.setMatchId(rs.getInt("matchId"));
        match.setPlayer1Id(rs.getInt("player1Id"));
        match.setPlayer2Id(rs.getInt("player2Id"));
        match.setPlayer1Score(rs.getInt("player1Score"));
        match.setPlayer2Score(rs.getInt("player2Score"));

        int winnerId = rs.getInt("winnerId");
        match.setWinnerId(rs.wasNull() ? null : winnerId);

        match.setMatchStatus(rs.getString("matchStatus"));
        match.setStartTime(rs.getTimestamp("startTime"));
        match.setEndTime(rs.getTimestamp("endTime"));
        match.setCreatedAt(rs.getTimestamp("createdAt"));
        return match;
    }

    /**
     * Helper method: Extract MatchEntity từ ResultSet
     */
    private MatchEntity extractMatchEntityFromResultSet(ResultSet rs) throws SQLException {
        MatchEntity entity = new MatchEntity();
        entity.setMatchId(rs.getInt("matchId"));
        entity.setPlayer1Id(rs.getInt("player1Id"));
        entity.setPlayer1Username(rs.getString("player1Username"));
        entity.setPlayer1DisplayName(rs.getString("player1DisplayName"));
        entity.setPlayer1Score(rs.getInt("player1Score"));

        entity.setPlayer2Id(rs.getInt("player2Id"));
        entity.setPlayer2Username(rs.getString("player2Username"));
        entity.setPlayer2DisplayName(rs.getString("player2DisplayName"));
        entity.setPlayer2Score(rs.getInt("player2Score"));

        int winnerId = rs.getInt("winnerId");
        entity.setWinnerId(rs.wasNull() ? null : winnerId);
        entity.setWinnerName(rs.getString("winnerName"));

        entity.setMatchStatus(rs.getString("matchStatus"));
        entity.setStartTime(rs.getTimestamp("startTime"));
        entity.setEndTime(rs.getTimestamp("endTime"));
        entity.setDurationMinutes(rs.getInt("durationMinutes"));
        return entity;
    }

    /**
     * Helper method: Extract UserHistoryMatch từ ResultSet
     */
    private UserHistoryMatch extractHistoryFromResultSet(ResultSet rs) throws SQLException {
        UserHistoryMatch history = new UserHistoryMatch();
        history.setHistoryId(rs.getInt("historyId"));
        history.setUserId(rs.getInt("userId"));
        history.setMatchId(rs.getInt("matchId"));
        history.setResult(rs.getString("result"));
        history.setTime(rs.getTimestamp("time"));
        return history;
    }

    /**
     * Helper method: Đóng resources
     */
    private void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) dbConnection.releaseConnection(conn);
        } catch (SQLException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }
}
