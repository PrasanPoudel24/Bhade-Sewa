package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/feed")
public class FeedServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // ✅ ADDED: Check if user is logged in (optional but good for personalization)
        HttpSession session = request.getSession(false);
        Integer userId = null;
        if (session != null && session.getAttribute("userId") != null) {
            userId = (Integer) session.getAttribute("userId");
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            
            // Query to get all posts
            String sql = "SELECT p.id, p.title, p.category, p.price, p.location, p.description, p.created_at, " +
                         "(SELECT image_url FROM images WHERE post_id=p.id LIMIT 1) AS image_url, " +
                         "u.name AS owner_name, u.id AS owner_id " +
                         "FROM posts p JOIN users u ON p.owner_id=u.id ORDER BY p.created_at DESC";
            
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                StringBuilder json = new StringBuilder();
                json.append("[");

                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    first = false;

                    int postId = rs.getInt("id");
                    
                    // ✅ ADDED: Check if current user has saved this post
                    boolean isSaved = false;
                    if (userId != null) {
                        try {
                            String savedSql = "SELECT COUNT(*) FROM saved_posts WHERE user_id = ? AND post_id = ?";
                            PreparedStatement savedPs = conn.prepareStatement(savedSql);
                            savedPs.setInt(1, userId);
                            savedPs.setInt(2, postId);
                            ResultSet savedRs = savedPs.executeQuery();
                            if (savedRs.next()) {
                                isSaved = savedRs.getInt(1) > 0;
                            }
                            savedRs.close();
                            savedPs.close();
                        } catch (SQLException e) {
                            // If saved_posts table doesn't exist, ignore
                        }
                    }

                    json.append("{")
                        .append("\"id\":").append(postId).append(",")
                        .append("\"title\":\"").append(escapeJson(rs.getString("title"))).append("\",")
                        .append("\"category\":\"").append(escapeJson(rs.getString("category"))).append("\",")
                        .append("\"price\":").append(rs.getDouble("price")).append(",")
                        .append("\"location\":\"").append(escapeJson(rs.getString("location"))).append("\",")
                        .append("\"description\":\"").append(escapeJson(rs.getString("description"))).append("\",")
                        .append("\"createdAt\":\"").append(escapeJson(rs.getString("created_at"))).append("\",")
                        .append("\"ownerName\":\"").append(escapeJson(rs.getString("owner_name"))).append("\",")
                        .append("\"ownerId\":").append(rs.getInt("owner_id")).append(",")
                        .append("\"isSaved\":").append(isSaved).append(",");

                    String imageUrl = rs.getString("image_url");
                    if (imageUrl == null || imageUrl.isEmpty())
                        imageUrl = "images/default.jpg";
                    json.append("\"imageUrl\":\"").append(escapeJson(imageUrl)).append("\"}");
                }

                json.append("]");
                out.print(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}