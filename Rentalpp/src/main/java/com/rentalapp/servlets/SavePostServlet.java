package com.rentalapp.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import java.io.*;
import java.sql.*;

@WebServlet("/savePost")
public class SavePostServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        PrintWriter out = response.getWriter();
        
        System.out.println("=== SavePostServlet called ===");

        // Check session first
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("ERROR: No session found");
            out.write("{\"success\":false,\"message\":\"Please login to save posts\"}");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            System.out.println("ERROR: User ID not found in session");
            out.write("{\"success\":false,\"message\":\"User session expired. Please login again.\"}");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        System.out.println("User ID from session: " + userId);

        // Read request body
        StringBuilder requestBody = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        }
        
        String body = requestBody.toString();
        System.out.println("Request body: " + body);

        // Parse postId from JSON
        int postId = 0;
        try {
            if (body.contains("\"postId\"")) {
                // Extract postId from JSON like {"postId":123}
                int start = body.indexOf("\"postId\"") + 8;
                int end = body.indexOf("}", start);
                if (end == -1) end = body.length();
                
                String numberPart = body.substring(start, end).replaceAll("[^0-9]", "");
                postId = Integer.parseInt(numberPart);
            } else if (body.matches("\\d+")) {
                postId = Integer.parseInt(body.trim());
            } else {
                throw new Exception("Invalid format");
            }
            System.out.println("Post ID to save: " + postId);
        } catch (Exception e) {
            System.out.println("ERROR: Failed to parse postId: " + e.getMessage());
            out.write("{\"success\":false,\"message\":\"Invalid post ID\"}");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("ERROR: MySQL Driver not found");
            out.write("{\"success\":false,\"message\":\"Database error\"}");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Database connection established");
            
            // Check if post exists
            String checkSql = "SELECT id, owner_id FROM posts WHERE id = ?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, postId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Post not found: " + postId);
                        out.write("{\"success\":false,\"message\":\"Post not found\"}");
                        return;
                    }
                    
                    int ownerId = rs.getInt("owner_id");
                    if (ownerId == userId) {
                        System.out.println("User trying to save own post");
                        out.write("{\"success\":false,\"message\":\"You cannot save your own post\"}");
                        return;
                    }
                }
            }

            // Save the post
            String insertSql = "INSERT INTO saved_posts (user_id, post_id) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, postId);
                
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("Post saved successfully");
                    out.write("{\"success\":true,\"message\":\"Post saved successfully!\"}");
                } else {
                    out.write("{\"success\":false,\"message\":\"Failed to save post\"}");
                }
            }
            
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Post already saved");
            out.write("{\"success\":false,\"message\":\"Post already saved\"}");
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
            out.write("{\"success\":false,\"message\":\"Database error: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setStatus(HttpServletResponse.SC_OK);
    }
}