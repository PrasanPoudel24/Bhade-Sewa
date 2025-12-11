package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/debugRequests")
public class DebugRequestsServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("=== RENTAL REQUESTS DEBUG INFORMATION ===\n");
        
        // Check session
        HttpSession session = request.getSession(false);
        if (session != null) {
            out.println("SESSION INFORMATION:");
            out.println("  Session ID: " + session.getId());
            out.println("  User ID in session: " + session.getAttribute("userId"));
            out.println();
        } else {
            out.println("NO ACTIVE SESSION (user not logged in)\n");
        }
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            
            out.println("DATABASE CONNECTION: SUCCESS\n");
            
            // 1. Check rent_requests table
            out.println("1. rent_requests TABLE:");
            out.println("   -------------------");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM rent_requests ORDER BY created_at DESC");
            
            int requestCount = 0;
            while (rs.next()) {
                requestCount++;
                out.println("   Request #" + requestCount + ":");
                out.println("     ID: " + rs.getInt("id"));
                out.println("     Renter ID: " + rs.getInt("renter_id"));
                out.println("     Owner ID: " + rs.getInt("owner_id"));
                out.println("     Post ID: " + rs.getInt("post_id"));
                out.println("     Status: " + rs.getString("status"));
                out.println("     Message: " + rs.getString("message"));
                out.println("     Created: " + rs.getTimestamp("created_at"));
                out.println();
            }
            
            if (requestCount == 0) {
                out.println("   No records found in rent_requests table\n");
            } else {
                out.println("   Total records: " + requestCount + "\n");
            }
            
            // 2. Check posts table
            out.println("2. posts TABLE:");
            out.println("   ------------");
            rs = stmt.executeQuery("SELECT id, title, owner_id, category, price FROM posts ORDER BY id");
            
            int postCount = 0;
            while (rs.next()) {
                postCount++;
                out.println("   Post #" + postCount + ":");
                out.println("     ID: " + rs.getInt("id"));
                out.println("     Title: " + rs.getString("title"));
                out.println("     Owner ID: " + rs.getInt("owner_id"));
                out.println("     Category: " + rs.getString("category"));
                out.println("     Price: Rs. " + rs.getDouble("price"));
                out.println();
            }
            
            out.println("   Total posts: " + postCount + "\n");
            
            // 3. Check users table
            out.println("3. users TABLE:");
            out.println("   ------------");
            rs = stmt.executeQuery("SELECT id, name, email, phone FROM users ORDER BY id");
            
            int userCount = 0;
            while (rs.next()) {
                userCount++;
                out.println("   User #" + userCount + ":");
                out.println("     ID: " + rs.getInt("id"));
                out.println("     Name: " + rs.getString("name"));
                out.println("     Email: " + rs.getString("email"));
                out.println("     Phone: " + rs.getString("phone"));
                out.println();
            }
            
            out.println("   Total users: " + userCount + "\n");
            
            // 4. Check data integrity
            out.println("4. DATA INTEGRITY CHECK:");
            out.println("   --------------------");
            
            // Check if all rent_requests have valid posts
            rs = stmt.executeQuery(
                "SELECT rr.id as request_id, rr.post_id, p.id as post_exists " +
                "FROM rent_requests rr " +
                "LEFT JOIN posts p ON rr.post_id = p.id"
            );
            
            out.println("   Rent_requests with missing posts:");
            int missingPosts = 0;
            while (rs.next()) {
                if (rs.getObject("post_exists") == null) {
                    missingPosts++;
                    out.println("     Request ID: " + rs.getInt("request_id") + 
                               " references non-existent Post ID: " + rs.getInt("post_id"));
                }
            }
            
            if (missingPosts == 0) {
                out.println("     All rent_requests have valid posts ✓");
            }
            out.println();
            
            // Check if all rent_requests have valid renters
            rs = stmt.executeQuery(
                "SELECT rr.id as request_id, rr.renter_id, u.id as user_exists " +
                "FROM rent_requests rr " +
                "LEFT JOIN users u ON rr.renter_id = u.id"
            );
            
            out.println("   Rent_requests with missing renters:");
            int missingRenters = 0;
            while (rs.next()) {
                if (rs.getObject("user_exists") == null) {
                    missingRenters++;
                    out.println("     Request ID: " + rs.getInt("request_id") + 
                               " references non-existent Renter ID: " + rs.getInt("renter_id"));
                }
            }
            
            if (missingRenters == 0) {
                out.println("     All rent_requests have valid renters ✓");
            }
            out.println();
            
            // 5. Test the JOIN query
            out.println("5. TEST JOIN QUERY:");
            out.println("   ---------------");
            
            if (session != null && session.getAttribute("userId") != null) {
                int userId = (int) session.getAttribute("userId");
                out.println("   Testing for user ID: " + userId);
                
                String sql = "SELECT " +
                    "rr.id, " +
                    "rr.post_id, " +
                    "rr.renter_id, " +
                    "rr.owner_id, " +
                    "rr.message, " +
                    "rr.status, " +
                    "p.title AS post_title, " +
                    "u.name AS renter_name " +
                    "FROM rent_requests rr " +
                    "LEFT JOIN posts p ON rr.post_id = p.id " +
                    "LEFT JOIN users u ON rr.renter_id = u.id " +
                    "WHERE rr.owner_id = ?";
                
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, userId);
                rs = ps.executeQuery();
                
                int joinCount = 0;
                while (rs.next()) {
                    joinCount++;
                    out.println("   Result #" + joinCount + ":");
                    out.println("     Request ID: " + rs.getInt("id"));
                    out.println("     Post ID: " + rs.getInt("post_id"));
                    out.println("     Post Title: " + rs.getString("post_title"));
                    out.println("     Renter ID: " + rs.getInt("renter_id"));
                    out.println("     Renter Name: " + rs.getString("renter_name"));
                    out.println("     Status: " + rs.getString("status"));
                    out.println();
                }
                
                if (joinCount == 0) {
                    out.println("   No results found for this user");
                } else {
                    out.println("   Total results: " + joinCount);
                }
            } else {
                out.println("   Cannot test JOIN query - no user logged in");
            }
            
            out.println("\n=== END OF DEBUG INFORMATION ===");
            
        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
            e.printStackTrace(out);
        }
    }
}