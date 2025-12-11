package com.rentalapp.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import java.io.*;
import java.sql.*;

@WebServlet("/removeSavedPost")
public class RemoveSavedPostServlet extends HttpServlet {
    
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
        
        System.out.println("=== RemoveSavedPostServlet called ===");
        
        // Check session
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("ERROR: No session found");
            out.write("{\"success\":false,\"message\":\"Please login to remove saved posts\"}");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            System.out.println("ERROR: User ID not found in session");
            out.write("{\"success\":false,\"message\":\"User session expired\"}");
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
        
        // Parse postId
        int postId = 0;
        try {
            if (body.contains("\"postId\"")) {
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
            System.out.println("Post ID to remove: " + postId);
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
            
            String sql = "DELETE FROM saved_posts WHERE user_id = ? AND post_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, postId);
                
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("Post removed successfully");
                    out.write("{\"success\":true,\"message\":\"Post removed from saved list\"}");
                } else {
                    System.out.println("Post not found in saved list");
                    out.write("{\"success\":false,\"message\":\"Post not found in your saved list\"}");
                }
            }
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