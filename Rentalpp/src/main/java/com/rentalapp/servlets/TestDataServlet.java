package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/testData")
public class TestDataServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("=== CREATE TEST DATA FOR RENT REQUESTS ===\n");
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            out.println("Please login first (need to know user ID)");
            return;
        }
        
        int currentUserId = (int) session.getAttribute("userId");
        out.println("Current logged-in user ID: " + currentUserId);
        out.println("Current user name: " + session.getAttribute("name"));
        out.println("Current user role: " + session.getAttribute("role"));
        out.println();
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            
            out.println("1. Checking current user's posts:");
            String sqlPosts = "SELECT id, title FROM posts WHERE owner_id = ?";
            PreparedStatement psPosts = conn.prepareStatement(sqlPosts);
            psPosts.setInt(1, currentUserId);
            ResultSet rsPosts = psPosts.executeQuery();
            
            int postCount = 0;
            while (rsPosts.next()) {
                postCount++;
                out.println("   Post #" + postCount + ": ID=" + rsPosts.getInt("id") + 
                           ", Title=" + rsPosts.getString("title"));
            }
            
            if (postCount == 0) {
                out.println("   No posts found for user ID " + currentUserId);
                out.println("   Creating a test post...");
                
                String insertPost = "INSERT INTO posts (title, description, category, price, location, owner_id) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement psInsert = conn.prepareStatement(insertPost, Statement.RETURN_GENERATED_KEYS);
                psInsert.setString(1, "Test Property for Requests");
                psInsert.setString(2, "This is a test property to receive rental requests");
                psInsert.setString(3, "House");
                psInsert.setDouble(4, 15000);
                psInsert.setString(5, "Kathmandu");
                psInsert.setInt(6, currentUserId);
                
                int rows = psInsert.executeUpdate();
                if (rows > 0) {
                    ResultSet keys = psInsert.getGeneratedKeys();
                    if (keys.next()) {
                        int postId = keys.getInt(1);
                        out.println("   Created test post with ID: " + postId);
                    }
                }
            }
            out.println();
            
            out.println("2. Finding another user to act as renter:");
            String sqlOtherUsers = "SELECT id, name FROM users WHERE id != ? AND role = 'renter' LIMIT 1";
            PreparedStatement psOther = conn.prepareStatement(sqlOtherUsers);
            psOther.setInt(1, currentUserId);
            ResultSet rsOther = psOther.executeQuery();
            
            int renterId = 0;
            String renterName = "";
            
            if (rsOther.next()) {
                renterId = rsOther.getInt("id");
                renterName = rsOther.getString("name");
                out.println("   Found renter: ID=" + renterId + ", Name=" + renterName);
            } else {
                out.println("   No other users found. Please create a renter account first.");
                out.println("   OR use existing user ID 1 if available.");
                out.println("   Manually run: INSERT INTO rent_requests (renter_id, owner_id, post_id, message) VALUES (1, " + currentUserId + ", [POST_ID], 'Test request')");
                return;
            }
            out.println();
            
            out.println("3. Creating a test rent request:");
            // Get a post ID owned by current user
            rsPosts = psPosts.executeQuery();
            if (rsPosts.next()) {
                int postId = rsPosts.getInt("id");
                String postTitle = rsPosts.getString("title");
                
                out.println("   Using post ID: " + postId + " (" + postTitle + ")");
                
                String sqlInsertRequest = "INSERT INTO rent_requests (renter_id, owner_id, post_id, message, status) VALUES (?, ?, ?, ?, 'pending')";
                PreparedStatement psRequest = conn.prepareStatement(sqlInsertRequest, Statement.RETURN_GENERATED_KEYS);
                psRequest.setInt(1, renterId);
                psRequest.setInt(2, currentUserId);
                psRequest.setInt(3, postId);
                psRequest.setString(4, "Test rental request from " + renterName);
                
                int requestRows = psRequest.executeUpdate();
                if (requestRows > 0) {
                    ResultSet requestKeys = psRequest.getGeneratedKeys();
                    if (requestKeys.next()) {
                        int requestId = requestKeys.getInt(1);
                        out.println("   SUCCESS: Created rent request with ID: " + requestId);
                        out.println("   Renter ID: " + renterId);
                        out.println("   Owner ID: " + currentUserId);
                        out.println("   Post ID: " + postId);
                        out.println("   Status: pending");
                    }
                } else {
                    out.println("   Failed to create rent request");
                }
            } else {
                out.println("   No posts available to create request");
            }
            out.println();
            
            out.println("4. Verifying the request was created:");
            String sqlVerify = "SELECT * FROM rent_requests WHERE owner_id = ?";
            PreparedStatement psVerify = conn.prepareStatement(sqlVerify);
            psVerify.setInt(1, currentUserId);
            ResultSet rsVerify = psVerify.executeQuery();
            
            int verifyCount = 0;
            while (rsVerify.next()) {
                verifyCount++;
                out.println("   Request #" + verifyCount + ":");
                out.println("     ID: " + rsVerify.getInt("id"));
                out.println("     Renter ID: " + rsVerify.getInt("renter_id"));
                out.println("     Owner ID: " + rsVerify.getInt("owner_id"));
                out.println("     Post ID: " + rsVerify.getInt("post_id"));
                out.println("     Status: " + rsVerify.getString("status"));
                out.println("     Message: " + rsVerify.getString("message"));
            }
            
            out.println("\nTotal requests for user ID " + currentUserId + ": " + verifyCount);
            
            if (verifyCount > 0) {
                out.println("\n=== TEST INSTRUCTIONS ===");
                out.println("1. Go to: http://localhost:8080/RentalApp/request.html");
                out.println("2. You should now see " + verifyCount + " request(s)");
                out.println("3. Check browser console (F12) for any errors");
            }
            
        } catch (Exception e) {
            out.println("ERROR: " + e.getMessage());
            e.printStackTrace(out);
        }
    }
}