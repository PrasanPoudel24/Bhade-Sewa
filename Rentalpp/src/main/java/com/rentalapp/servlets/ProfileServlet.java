package com.rentalapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

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

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            String sql = "SELECT id, name, email, phone, role, province, district, municipal, ward_num, created_at " +
                         "FROM users WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String name = escapeJson(rs.getString("name"));
                String email = escapeJson(rs.getString("email"));
                String phone = escapeJson(rs.getString("phone"));
                String role = escapeJson(rs.getString("role"));
                String province = escapeJson(rs.getString("province"));
                String district = escapeJson(rs.getString("district"));
                String municipal = escapeJson(rs.getString("municipal"));
                int wardNum = rs.getInt("ward_num");

                Timestamp createdAt = rs.getTimestamp("created_at");
                String formattedDate = "";
                if (createdAt != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm");
                    formattedDate = sdf.format(new Date(createdAt.getTime()));
                }

                StringBuilder json = new StringBuilder();
                json.append("{")
                    .append("\"id\":").append(id).append(",")
                    .append("\"name\":\"").append(name).append("\",")
                    .append("\"email\":\"").append(email).append("\",")
                    .append("\"phone\":\"").append(phone).append("\",")
                    .append("\"role\":\"").append(role).append("\",")
                    .append("\"province\":\"").append(province).append("\",")
                    .append("\"district\":\"").append(district).append("\",")
                    .append("\"municipal\":\"").append(municipal).append("\",")
                    .append("\"ward_num\":").append(wardNum).append(",")
                    .append("\"created_at\":\"").append(formattedDate).append("\"")
                    .append("}");

                out.print(json.toString());
                System.out.println("Profile data sent for user: " + name);

            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\":\"User not found\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Database error: " + e.getMessage() + "\"}");
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