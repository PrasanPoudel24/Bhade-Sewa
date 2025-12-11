package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/postdetail")
public class PostDetailsServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // ✅ ADDED: Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Please login to view post details\"}");
            return;
        }

        String postIdStr = request.getParameter("id");
        if (postIdStr == null || postIdStr.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Post ID is missing\"}");
            return;
        }

        int postId;
        try {
            postId = Integer.parseInt(postIdStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Invalid Post ID\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement psPost = null;
        ResultSet rsPost = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            // 1️⃣ Fetch post + owner details
            String sqlPost = "SELECT p.id, p.title, p.category, p.price, p.location, p.description, p.created_at, " +
                             "u.id AS ownerId, u.phone AS ownerPhone, u.name AS ownerName " +
                             "FROM posts p JOIN users u ON p.owner_id = u.id WHERE p.id = ?";
            psPost = conn.prepareStatement(sqlPost);
            psPost.setInt(1, postId);
            rsPost = psPost.executeQuery();

            if (!rsPost.next()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\":\"Post not found\"}");
                return;
            }

            int ownerId = rsPost.getInt("ownerId");
            String ownerPhone = rsPost.getString("ownerPhone");
            String ownerName = rsPost.getString("ownerName");
            String title = rsPost.getString("title");
            String category = rsPost.getString("category");
            double price = rsPost.getDouble("price");
            String location = rsPost.getString("location");
            String description = rsPost.getString("description");
            Timestamp createdAt = rsPost.getTimestamp("created_at");

            // 2️⃣ Fetch ALL images for this post
            List<String> allImages = new ArrayList<>();
            String sqlImages = "SELECT image_url FROM images WHERE post_id = ? ORDER BY id";
            PreparedStatement psImages = conn.prepareStatement(sqlImages);
            psImages.setInt(1, postId);
            ResultSet rsImages = psImages.executeQuery();
            
            while (rsImages.next()) {
                String imageUrl = rsImages.getString("image_url");
                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    allImages.add(imageUrl);
                }
            }
            rsImages.close();
            psImages.close();
            
            // If no images found, add a default
            if (allImages.isEmpty()) {
                allImages.add("images/default.jpg");
            }

            // ✅ ADDED: Get current user ID from session
            int currentUserId = (Integer) session.getAttribute("userId");
            
            // 3️⃣ Check if current user has saved this post
            boolean isSaved = false;
            try {
                String sqlSaved = "SELECT COUNT(*) FROM saved_posts WHERE user_id = ? AND post_id = ?";
                PreparedStatement psSaved = conn.prepareStatement(sqlSaved);
                psSaved.setInt(1, currentUserId);
                psSaved.setInt(2, postId);
                ResultSet rsSaved = psSaved.executeQuery();
                if (rsSaved.next()) {
                    isSaved = rsSaved.getInt(1) > 0;
                }
                rsSaved.close();
                psSaved.close();
            } catch (SQLException e) {
                // If saved_posts table doesn't exist, just ignore
                System.out.println("Saved posts check skipped: " + e.getMessage());
            }

            // 4️⃣ Build JSON with all details
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"id\":").append(postId).append(",");
            json.append("\"title\":\"").append(escapeJson(title)).append("\",");
            json.append("\"category\":\"").append(escapeJson(category)).append("\",");
            json.append("\"price\":").append(price).append(",");
            json.append("\"location\":\"").append(escapeJson(location)).append("\",");
            json.append("\"description\":\"").append(escapeJson(description)).append("\",");
            json.append("\"postedDate\":\"").append(createdAt.toString()).append("\",");
            json.append("\"ownerPhone\":\"").append(escapeJson(ownerPhone)).append("\",");
            json.append("\"ownerName\":\"").append(escapeJson(ownerName)).append("\",");
            json.append("\"ownerId\":").append(ownerId).append(",");
            json.append("\"currentUserId\":").append(currentUserId).append(",");
            json.append("\"isSaved\":").append(isSaved).append(",");
            
            // Add allImages array
            json.append("\"allImages\":[");
            for (int i = 0; i < allImages.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(escapeJson(allImages.get(i))).append("\"");
            }
            json.append("],");
            
            // Also keep single image for backward compatibility
            json.append("\"image\":\"").append(escapeJson(allImages.get(0))).append("\"");
            json.append("}");

            out.print(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            try { if (rsPost != null) rsPost.close(); } catch (Exception ignored) {}
            try { if (psPost != null) psPost.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}