package com.rentalapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/delete-post")
public class DeletePostServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);

        // Check if user is logged in
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\":false,\"error\":\"User not logged in\"}");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        try {
            int postId = Integer.parseInt(request.getParameter("postId"));

            Connection conn = null;
            PreparedStatement stmt = null;

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

                // Check if post belongs to user before deleting
                String checkSql = "SELECT id FROM posts WHERE id = ? AND owner_id = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, postId);
                checkStmt.setInt(2, userId);
                ResultSet rs = checkStmt.executeQuery();

                if (!rs.next()) {
                    out.print("{\"success\":false,\"error\":\"Post not found or unauthorized\"}");
                    return;
                }

                // Delete post (images will be deleted automatically due to CASCADE)
                String sql = "DELETE FROM posts WHERE id = ? AND owner_id = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, postId);
                stmt.setInt(2, userId);

                int rowsDeleted = stmt.executeUpdate();
                if (rowsDeleted > 0) {
                    out.print("{\"success\":true}");
                } else {
                    out.print("{\"success\":false,\"error\":\"Failed to delete post\"}");
                }

            } catch (Exception e) {
                e.printStackTrace();
                out.print("{\"success\":false,\"error\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
            } finally {
                try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
                try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"success\":false,\"error\":\"Invalid data format\"}");
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