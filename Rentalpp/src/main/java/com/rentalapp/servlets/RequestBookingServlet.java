package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/requestBooking")
public class RequestBookingServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        System.out.println("\n=== RequestBookingServlet ===");
        
        // Check session
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            out.write("{\"success\":false,\"message\":\"Please login first\"}");
            return;
        }
        
        int renterId = (int) session.getAttribute("userId");
        String renterName = (String) session.getAttribute("name");
        String userRole = (String) session.getAttribute("role");
        
        System.out.println("Renter - ID: " + renterId + ", Name: " + renterName + ", Role: " + userRole);
        
        // Only renters can send requests
        if (!"renter".equals(userRole)) {
            out.write("{\"success\":false,\"message\":\"Only renters can send booking requests\"}");
            return;
        }
        
        try {
            // Read request body
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            
            System.out.println("Received JSON: " + json);
            
            // Parse JSON manually
            int postId = 0;
            String message = "";
            
            // Simple JSON parsing
            json = json.trim().replace("{", "").replace("}", "");
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim().replace("\"", "");
                    
                    if ("postId".equals(key)) {
                        try {
                            postId = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid postId: " + value);
                        }
                    } else if ("message".equals(key)) {
                        message = value;
                    }
                }
            }
            
            System.out.println("Parsed - Post ID: " + postId + ", Message: " + message);
            
            if (postId <= 0) {
                out.write("{\"success\":false,\"message\":\"Invalid property ID\"}");
                return;
            }
            
            if (message == null || message.isEmpty()) {
                message = "Request to book your rental.";
            }
            
            // Process the request
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                conn.setAutoCommit(false);
                
                // 1. Get post details
                String sqlPost = "SELECT owner_id, title FROM posts WHERE id = ?";
                PreparedStatement psPost = conn.prepareStatement(sqlPost);
                psPost.setInt(1, postId);
                ResultSet rsPost = psPost.executeQuery();
                
                if (!rsPost.next()) {
                    out.write("{\"success\":false,\"message\":\"Property not found\"}");
                    return;
                }
                
                int ownerId = rsPost.getInt("owner_id");
                String postTitle = rsPost.getString("title");
                
                System.out.println("Post Owner ID: " + ownerId + ", Title: " + postTitle);
                
                // 2. Check if renter is trying to request their own property
                if (renterId == ownerId) {
                    out.write("{\"success\":false,\"message\":\"You cannot request your own property\"}");
                    return;
                }
                
                // 3. Check if request already exists
                String sqlCheck = "SELECT status FROM rent_requests WHERE renter_id = ? AND post_id = ?";
                PreparedStatement psCheck = conn.prepareStatement(sqlCheck);
                psCheck.setInt(1, renterId);
                psCheck.setInt(2, postId);
                ResultSet rsCheck = psCheck.executeQuery();
                
                if (rsCheck.next()) {
                    String status = rsCheck.getString("status");
                    out.write("{\"success\":false,\"message\":\"You already have a " + status + " request for this property\"}");
                    return;
                }
                
                // 4. Insert the request
                String sqlInsert = "INSERT INTO rent_requests (renter_id, owner_id, post_id, message) VALUES (?, ?, ?, ?)";
                PreparedStatement psInsert = conn.prepareStatement(sqlInsert);
                psInsert.setInt(1, renterId);
                psInsert.setInt(2, ownerId);
                psInsert.setInt(3, postId);
                psInsert.setString(4, message);
                
                int rows = psInsert.executeUpdate();
                
                if (rows > 0) {
                    conn.commit();
                    System.out.println("Rental request created successfully");
                    out.write("{\"success\":true,\"message\":\"Booking request sent successfully!\"}");
                } else {
                    conn.rollback();
                    out.write("{\"success\":false,\"message\":\"Failed to send request\"}");
                }
                
            } catch (SQLException e) {
                System.out.println("SQL Error: " + e.getMessage());
                out.write("{\"success\":false,\"message\":\"Database error: " + e.getMessage() + "\"}");
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            out.write("{\"success\":false,\"message\":\"Error: " + e.getMessage() + "\"}");
        }
    }
}