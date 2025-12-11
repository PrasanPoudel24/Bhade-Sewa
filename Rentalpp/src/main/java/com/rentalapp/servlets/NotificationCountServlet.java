package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/notificationCount")
public class NotificationCountServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"count\":0}");
            return;
        }
        
        int userId = (int) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("role");
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            
            int unreadCount = 0;
            
            if ("owner".equals(userRole)) {
                String sql = "SELECT COUNT(*) as count FROM rent_requests WHERE owner_id = ? AND status IN ('pending', 'approved')";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    unreadCount = rs.getInt("count");
                }
            } else if ("renter".equals(userRole)) {
                String sql = "SELECT COUNT(*) as count FROM rent_requests WHERE renter_id = ? AND status IN ('approved', 'rejected')";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    unreadCount = rs.getInt("count");
                }
            }
            
            response.getWriter().write("{\"count\":" + unreadCount + "}");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{\"count\":0}");
        }
    }
}