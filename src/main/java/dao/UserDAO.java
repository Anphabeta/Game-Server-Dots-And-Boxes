package dao;

import model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho bảng User
 * @author Phạm Tiến Dương
 */
public class UserDAO {

    private DBConnection dbConnection;

    public UserDAO() {
        this.dbConnection = DBConnection.getInstance();
    }

    /**
     * Tìm user theo username và password (dùng cho login)
     * @return User object nếu tìm thấy, null nếu không
     */
    public User findByUsernameAndPassword(String username, String password) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT * FROM User WHERE username = ? AND password = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);

            rs = ps.executeQuery();

            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error in findByUsernameAndPassword: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return null;
    }

    /**
     * Tìm user theo userId
     */
    public User findById(int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT * FROM User WHERE userId = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);

            rs = ps.executeQuery();

            if (rs.next()) {
                return extractUserFromResultSet(rs);
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
     * Tìm user theo username
     */
    public User findByUsername(String username) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT * FROM User WHERE username = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);

            rs = ps.executeQuery();

            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error in findByUsername: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return null;
    }

    /**
     * Tạo user mới (đăng ký)
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean createUser(User user) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "INSERT INTO User (username, password, displayName, ranking, status) " +
                        "VALUES (?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getDisplayName());
            ps.setInt(4, user.getRanking());
            ps.setString(5, user.getStatus());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                // Lấy userId được tạo tự động
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error in createUser: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, ps, conn);
        }

        return false;
    }

    /**
     * Cập nhật thông tin user
     */
    public boolean updateUser(User user) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "UPDATE User SET username = ?, password = ?, displayName = ?, " +
                        "ranking = ?, status = ? WHERE userId = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getDisplayName());
            ps.setInt(4, user.getRanking());
            ps.setString(5, user.getStatus());
            ps.setInt(6, user.getUserId());

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error in updateUser: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, ps, conn);
        }

        return false;
    }

    /**
     * Cập nhật status của user
     */
    public boolean updateUserStatus(int userId, String status) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "UPDATE User SET status = ? WHERE userId = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setInt(2, userId);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error in updateUserStatus: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, ps, conn);
        }

        return false;
    }

    /**
     * Cập nhật ranking của user
     */
    public boolean updateUserRanking(int userId, int ranking) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "UPDATE User SET ranking = ? WHERE userId = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, ranking);
            ps.setInt(2, userId);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error in updateUserRanking: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, ps, conn);
        }

        return false;
    }

    /**
     * Xóa user
     */
    public boolean deleteUser(int userId) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "DELETE FROM User WHERE userId = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error in deleteUser: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(null, ps, conn);
        }

        return false;
    }

    /**
     * Lấy danh sách tất cả users
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT * FROM User ORDER BY ranking DESC";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error in getAllUsers: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, stmt, conn);
        }

        return users;
    }

    /**
     * Lấy danh sách users theo status
     */
    public List<User> getUsersByStatus(String status) {
        List<User> users = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT * FROM User WHERE status = ? ORDER BY ranking DESC";
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            rs = ps.executeQuery();

            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error in getUsersByStatus: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return users;
    }

    /**
     * Lấy top N users theo ranking
     */
    public List<User> getTopUsersByRanking(int limit) {
        List<User> users = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT * FROM User ORDER BY ranking DESC LIMIT ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, limit);
            rs = ps.executeQuery();

            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error in getTopUsersByRanking: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return users;
    }

    /**
     * Kiểm tra username đã tồn tại chưa
     */
    public boolean isUsernameExists(String username) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM User WHERE username = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error in isUsernameExists: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return false;
    }

    /**
     * Đếm tổng số users
     */
    public int getTotalUsers() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM User";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error in getTotalUsers: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, stmt, conn);
        }

        return 0;
    }

    /**
     * Helper method: Extract User object từ ResultSet
     */
    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("userId"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setDisplayName(rs.getString("displayName"));
        user.setRanking(rs.getInt("ranking"));
        user.setStatus(rs.getString("status"));
        user.setCreatedAt(rs.getTimestamp("createdAt"));
        user.setUpdatedAt(rs.getTimestamp("updatedAt"));
        return user;
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

    public boolean isValidPassword(String userName, String hashedPassword) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM User WHERE username = ? AND password = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, userName);
            ps.setString(2, hashedPassword);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error in isValidPassword: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, ps, conn);
        }

        return false;
    }
}
