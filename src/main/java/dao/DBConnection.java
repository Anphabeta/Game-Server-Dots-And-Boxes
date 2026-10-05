package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class quản lý kết nối Database với Connection Pool
 * @author Phạm Tiến Dương
 */
public class DBConnection {

    // Thông tin kết nối database
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dots_boxes_game";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    // Connection Pool settings
    private static final int INITIAL_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 20;

    // Singleton instance
    private static DBConnection instance;

    // Connection Pool
    private List<Connection> availableConnections;
    private List<Connection> usedConnections;

    /**
     * Private constructor để đảm bảo Singleton
     */
    private DBConnection() {
        availableConnections = new ArrayList<>();
        usedConnections = new ArrayList<>();

        // Load MySQL JDBC Driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Khởi tạo connection pool ban đầu
            for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
                availableConnections.add(createConnection());
            }

            System.out.println("DBConnection: Initialized connection pool with "
                    + INITIAL_POOL_SIZE + " connections");

        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Error initializing connection pool!");
            e.printStackTrace();
        }
    }

    /**
     * Lấy instance của DBConnection (Singleton)
     */
    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /**
     * Tạo một connection mới tới database
     */
    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Lấy một connection từ pool
     */
    public synchronized Connection getConnection() throws SQLException {
        // Nếu không còn connection available
        if (availableConnections.isEmpty()) {
            // Nếu chưa đạt max pool size, tạo connection mới
            if (usedConnections.size() < MAX_POOL_SIZE) {
                availableConnections.add(createConnection());
                System.out.println("DBConnection: Created new connection. Total connections: "
                        + (availableConnections.size() + usedConnections.size()));
            } else {
                // Đã đạt max pool size, phải đợi
                throw new SQLException("Maximum pool size reached. No available connections!");
            }
        }

        // Lấy connection từ available pool
        Connection connection = availableConnections.remove(availableConnections.size() - 1);

        // Kiểm tra connection còn valid không
        if (!connection.isValid(2)) {
            connection = createConnection();
            System.out.println("DBConnection: Recreated invalid connection");
        }

        // Chuyển sang used pool
        usedConnections.add(connection);

        return connection;
    }

    /**
     * Trả connection về pool sau khi sử dụng xong
     */
    public synchronized boolean releaseConnection(Connection connection) {
        if (connection == null) {
            return false;
        }

        // Xóa khỏi used pool
        boolean removed = usedConnections.remove(connection);

        if (removed) {
            // Thêm vào available pool
            availableConnections.add(connection);
            return true;
        }

        return false;
    }

    /**
     * Đóng một connection cụ thể
     */
    public synchronized void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                usedConnections.remove(connection);
                availableConnections.remove(connection);
                connection.close();
                System.out.println("DBConnection: Connection closed");
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Đóng tất cả connections trong pool (gọi khi shutdown ứng dụng)
     */
    public synchronized void closeAllConnections() {
        // Đóng tất cả available connections
        for (Connection conn : availableConnections) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing available connection: " + e.getMessage());
            }
        }
        availableConnections.clear();

        // Đóng tất cả used connections
        for (Connection conn : usedConnections) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing used connection: " + e.getMessage());
            }
        }
        usedConnections.clear();

        System.out.println("DBConnection: All connections closed");
    }

    /**
     * Lấy số lượng connection available
     */
    public synchronized int getAvailableConnectionsCount() {
        return availableConnections.size();
    }

    /**
     * Lấy số lượng connection đang được sử dụng
     */
    public synchronized int getUsedConnectionsCount() {
        return usedConnections.size();
    }

    /**
     * Lấy tổng số connection trong pool
     */
    public synchronized int getTotalConnectionsCount() {
        return availableConnections.size() + usedConnections.size();
    }

    /**
     * In thông tin pool (debug)
     */
    public synchronized void printPoolStatus() {
        System.out.println("=== Connection Pool Status ===");
        System.out.println("Available: " + availableConnections.size());
        System.out.println("Used: " + usedConnections.size());
        System.out.println("Total: " + getTotalConnectionsCount());
        System.out.println("Max Pool Size: " + MAX_POOL_SIZE);
        System.out.println("=============================");
    }

    /**
     * Test connection tới database
     */
    public static boolean testConnection() {
        try {
            Connection conn = getInstance().getConnection();
            boolean isValid = conn.isValid(2);
            getInstance().releaseConnection(conn);

            if (isValid) {
                System.out.println("Database connection test: SUCCESS");
            } else {
                System.out.println("Database connection test: FAILED");
            }

            return isValid;
        } catch (SQLException e) {
            System.err.println("Database connection test: FAILED");
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Main method để test DBConnection
     */
    public static void main() {
        System.out.println("Testing DBConnection...\n");

        // Test connection
        testConnection();

        // Print pool status
        getInstance().printPoolStatus();

        // Test lấy nhiều connection
        try {
            System.out.println("\nTesting multiple connections...");
            Connection conn1 = getInstance().getConnection();
            Connection conn2 = getInstance().getConnection();
            Connection conn3 = getInstance().getConnection();

            getInstance().printPoolStatus();

            // Release connections
            getInstance().releaseConnection(conn1);
            getInstance().releaseConnection(conn2);
            getInstance().releaseConnection(conn3);

            System.out.println("\nAfter releasing connections:");
            getInstance().printPoolStatus();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Cleanup
        getInstance().closeAllConnections();
    }
}
