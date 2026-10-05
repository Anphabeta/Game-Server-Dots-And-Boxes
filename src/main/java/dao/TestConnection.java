package dao;

import dao.DBConnection;
import dao.UserDAO;
import model.User;
import java.util.List;

public class TestConnection {
    public static void main() {
        System.out.println("=== Testing Database Connection ===\n");
        
        // Test 1: Connection
        System.out.println("1. Testing MySQL Connection...");
        boolean connected = DBConnection.testConnection();
        
        if (connected) {
            System.out.println("✓ Connection SUCCESS!\n");
            
            // Test 2: Connection Pool Status
            System.out.println("2. Connection Pool Status:");
            DBConnection.getInstance().printPoolStatus();
            
            // Test 3: UserDAO
            System.out.println("\n3. Testing UserDAO...");
            UserDAO userDAO = new UserDAO();
            
            // Get top 5 users
            System.out.println("Top 5 Users by Ranking:");
            List<User> topUsers = userDAO.getTopUsersByRanking(5);
            for (User u : topUsers) {
                System.out.println("  - " + u.getDisplayName() + 
                                 " (Ranking: " + u.getRanking() + 
                                 ", Status: " + u.getStatus() + ")");
            }
            
            System.out.println("\n✓ All tests passed!");
            
        } else {
            System.out.println("✗ Connection FAILED!");
            System.out.println("Please check:");
            System.out.println("  1. XAMPP MySQL is running");
            System.out.println("  2. Database 'dots_boxes_game' is created");
            System.out.println("  3. MySQL credentials are correct");
        }
        
        // Cleanup
        DBConnection.getInstance().closeAllConnections();
    }
}