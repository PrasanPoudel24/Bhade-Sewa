package com.rentalapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/user-posts")
public class UserPostsServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);

        // Check if user is logged in
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"User not logged in\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        System.out.println("Fetching posts for user ID: " + userId);

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            // Query to get posts for the logged-in user with image URL
            String sql = "SELECT p.id, p.title, p.description, p.price, p.location, " +
                        "p.category, p.created_at, " +
                        "(SELECT image_url FROM images WHERE post_id = p.id LIMIT 1) as image_url " +
                        "FROM posts p " +
                        "WHERE p.owner_id = ? " +
                        "ORDER BY p.created_at DESC";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            StringBuilder json = new StringBuilder();
            json.append("[");

            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }
                first = false;

                int id = rs.getInt("id");
                String title = escapeJson(rs.getString("title"));
                String description = escapeJson(rs.getString("description"));
                double price = rs.getDouble("price");
                String location = escapeJson(rs.getString("location"));
                String category = escapeJson(rs.getString("category"));
                String imageUrl = rs.getString("image_url");
                
                // If no image, use placeholder
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    imageUrl = "https://images.unsplash.com/photo-1518780664697-55e3ad937233?w=400&h=300&fit=crop";
                }
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                String formattedDate = "";
                if (createdAt != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm");
                    formattedDate = sdf.format(new Date(createdAt.getTime()));
                }

                json.append("{")
                    .append("\"id\":").append(id).append(",")
                    .append("\"title\":\"").append(title).append("\",")
                    .append("\"description\":\"").append(description).append("\",")
                    .append("\"price\":").append(price).append(",")
                    .append("\"location\":\"").append(location).append("\",")
                    .append("\"category\":\"").append(category).append("\",")
                    .append("\"image_url\":\"").append(escapeJson(imageUrl)).append("\",")
                    .append("\"created_at\":\"").append(formattedDate).append("\"")
                    .append("}");
            }

            json.append("]");
            
            // If no posts, return empty array
            if (first) {
                json = new StringBuilder("[]");
            }
            
            out.print(json.toString());
            System.out.println("Sent " + (first ? 0 : "some") + " posts for user ID: " + userId);

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
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