package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/getSavedPosts")
public class GetSavedPostsServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        
        System.out.println("=== GetSavedPostsServlet called ===");
        System.out.println("Session ID from request: " + request.getRequestedSessionId());

        // Get session WITHOUT creating new one
        HttpSession session = request.getSession(false);
        
        if (session == null) {
            System.out.println("ERROR: No session found");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\":\"Please login to view saved posts\"}");
            return;
        }
        
        Object userIdObj = session.getAttribute("userId");
        if (userIdObj == null) {
            System.out.println("ERROR: userId not found in session");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\":\"User session expired. Please login again.\"}");
            return;
        }
        
        int userId = (Integer) userIdObj;
        System.out.println("✓ User ID from session: " + userId);
        System.out.println("✓ Username: " + session.getAttribute("username"));

        String sql = 
            "SELECT p.id, p.title, p.price, p.location, p.category, p.description, " +
            "       (SELECT image_url FROM images WHERE post_id = p.id LIMIT 1) AS image_url, " +
            "       u.name AS owner_name " +
            "FROM saved_posts sp " +
            "INNER JOIN posts p ON sp.post_id = p.id " +
            "INNER JOIN users u ON p.owner_id = u.id " +
            "WHERE sp.user_id = ? " +
            "ORDER BY sp.saved_at DESC";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            out.write("{\"error\":\"Database configuration error\"}");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            System.out.println("Executing query for user ID: " + userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder json = new StringBuilder();
                json.append("[");
                
                boolean first = true;
                int count = 0;
                while (rs.next()) {
                    count++;
                    if (!first) json.append(",");
                    first = false;
                    
                    int postId = rs.getInt("id");
                    String title = rs.getString("title");
                    double price = rs.getDouble("price");
                    String location = rs.getString("location");
                    String category = rs.getString("category");
                    String description = rs.getString("description");
                    String imageUrl = rs.getString("image_url");
                    String ownerName = rs.getString("owner_name");
                    
                    // Process image URL
                    String image = "";
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        if (imageUrl.contains("/")) {
                            image = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                        } else if (imageUrl.contains("\\")) {
                            image = imageUrl.substring(imageUrl.lastIndexOf("\\") + 1);
                        } else {
                            image = imageUrl;
                        }
                    }
                    
                    // Build JSON for this post
                    json.append("{")
                        .append("\"id\":").append(postId).append(",")
                        .append("\"title\":\"").append(escapeJson(title != null ? title : "")).append("\",")
                        .append("\"price\":").append(price).append(",")
                        .append("\"priceType\":\"/month\",")
                        .append("\"location\":\"").append(escapeJson(location != null ? location : "")).append("\",")
                        .append("\"category\":\"").append(escapeJson(category != null ? category : "")).append("\",")
                        .append("\"description\":\"").append(escapeJson(description != null ? description : "")).append("\",")
                        .append("\"ownerName\":\"").append(escapeJson(ownerName != null ? ownerName : "")).append("\",")
                        .append("\"image\":\"").append(escapeJson(image)).append("\"")
                        .append("}");
                }
                
                json.append("]");
                
                System.out.println("✓ Total posts found: " + count);
                
                if (count == 0) {
                    System.out.println("No saved posts found for this user");
                }
                
                out.write(json.toString());
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}