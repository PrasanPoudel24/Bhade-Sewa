package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/notifications")
public class NotificationServlet extends HttpServlet {
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
            response.getWriter().write("{\"error\":\"Please login first\"}");
            return;
        }
        
        int userId = (int) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("role");
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            
            if ("owner".equals(userRole)) {
                fetchOwnerNotifications(userId, response, conn);
            } else if ("renter".equals(userRole)) {
                fetchRenterNotifications(userId, response, conn);
            } else {
                response.getWriter().write("{\"notifications\":[]}");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    private void fetchOwnerNotifications(int ownerId, HttpServletResponse response, Connection conn) 
            throws IOException, SQLException {
        
        String sql = "SELECT " +
            "rr.id AS request_id, " +
            "rr.post_id, " +
            "rr.renter_id, " +
            "rr.status, " +
            "rr.created_at, " +
            "rr.message, " +
            "p.title AS post_title, " +
            "p.category, " +
            "p.price, " +
            "p.location, " +
            "u.name AS renter_name, " +
            "u.email AS renter_email, " +
            "u.phone AS renter_phone, " +
            "u.province, " +
            "u.district, " +
            "u.municipal, " +
            "u.ward_num, " +
            "CASE " +
            "  WHEN rr.status = 'pending' THEN 'New booking request received' " +
            "  WHEN rr.status = 'approved' THEN 'You approved a booking request' " +
            "  WHEN rr.status = 'rejected' THEN 'You rejected a booking request' " +
            "END AS notification_title, " +
            "CASE " +
            "  WHEN rr.status = 'pending' THEN CONCAT(u.name, ' wants to book your property: ', p.title) " +
            "  WHEN rr.status = 'approved' THEN CONCAT('You approved ', u.name, '''s request for ', p.title) " +
            "  WHEN rr.status = 'rejected' THEN CONCAT('You rejected ', u.name, '''s request for ', p.title) " +
            "END AS notification_message, " +
            "CASE rr.status " +
            "  WHEN 'pending' THEN 'info' " +
            "  WHEN 'approved' THEN 'success' " +
            "  WHEN 'rejected' THEN 'warning' " +
            "END AS notification_type " +
            "FROM rent_requests rr " +
            "JOIN posts p ON rr.post_id = p.id " +
            "JOIN users u ON rr.renter_id = u.id " +
            "WHERE rr.owner_id = ? " +
            "ORDER BY rr.created_at DESC";
        
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, ownerId);
        ResultSet rs = ps.executeQuery();
        
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        
        while (rs.next()) {
            if (!first) json.append(",");
            first = false;
            
            String province = rs.getString("province");
            String district = rs.getString("district");
            String municipal = rs.getString("municipal");
            int wardNum = rs.getInt("ward_num");
            
            String address = buildAddress(province, district, municipal, wardNum);
            
            json.append("{")
                .append("\"id\":").append(rs.getInt("request_id")).append(",")
                .append("\"post_id\":").append(rs.getInt("post_id")).append(",")
                .append("\"renter_id\":").append(rs.getInt("renter_id")).append(",")
                .append("\"status\":\"").append(escapeJson(rs.getString("status"))).append("\",")
                .append("\"type\":\"").append(escapeJson(rs.getString("notification_type"))).append("\",")
                .append("\"title\":\"").append(escapeJson(rs.getString("notification_title"))).append("\",")
                .append("\"message\":\"").append(escapeJson(rs.getString("notification_message"))).append("\",")
                .append("\"post_title\":\"").append(escapeJson(rs.getString("post_title"))).append("\",")
                .append("\"category\":\"").append(escapeJson(rs.getString("category"))).append("\",")
                .append("\"price\":").append(rs.getDouble("price")).append(",")
                .append("\"location\":\"").append(escapeJson(rs.getString("location"))).append("\",")
                .append("\"renter_name\":\"").append(escapeJson(rs.getString("renter_name"))).append("\",")
                .append("\"renter_email\":\"").append(escapeJson(rs.getString("renter_email"))).append("\",")
                .append("\"renter_phone\":\"").append(escapeJson(rs.getString("renter_phone"))).append("\",")
                .append("\"renter_address\":\"").append(escapeJson(address)).append("\",")
                .append("\"request_message\":\"").append(escapeJson(rs.getString("message"))).append("\",")
                .append("\"created_at\":\"").append(rs.getTimestamp("created_at")).append("\"")
                .append("}");
        }
        
        json.append("]");
        response.getWriter().write(json.toString());
    }
    
    private void fetchRenterNotifications(int renterId, HttpServletResponse response, Connection conn) 
            throws IOException, SQLException {
        
        String sql = "SELECT " +
            "rr.id AS request_id, " +
            "rr.post_id, " +
            "rr.owner_id, " +
            "rr.status, " +
            "rr.created_at, " +
            "rr.message, " +
            "p.title AS post_title, " +
            "p.category, " +
            "p.price, " +
            "p.location, " +
            "u.name AS owner_name, " +
            "u.email AS owner_email, " +
            "u.phone AS owner_phone, " +
            "u.province, " +
            "u.district, " +
            "u.municipal, " +
            "u.ward_num, " +
            "CASE " +
            "  WHEN rr.status = 'pending' THEN 'Booking request sent' " +
            "  WHEN rr.status = 'approved' THEN 'Booking request approved!' " +
            "  WHEN rr.status = 'rejected' THEN 'Booking request declined' " +
            "END AS notification_title, " +
            "CASE " +
            "  WHEN rr.status = 'pending' THEN CONCAT('You requested to book ', p.title) " +
            "  WHEN rr.status = 'approved' THEN CONCAT('Your booking request for ', p.title, ' has been approved!') " +
            "  WHEN rr.status = 'rejected' THEN CONCAT('Your booking request for ', p.title, ' was declined') " +
            "END AS notification_message, " +
            "CASE rr.status " +
            "  WHEN 'pending' THEN 'info' " +
            "  WHEN 'approved' THEN 'success' " +
            "  WHEN 'rejected' THEN 'warning' " +
            "END AS notification_type " +
            "FROM rent_requests rr " +
            "JOIN posts p ON rr.post_id = p.id " +
            "JOIN users u ON rr.owner_id = u.id " +
            "WHERE rr.renter_id = ? " +
            "ORDER BY rr.created_at DESC";
        
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, renterId);
        ResultSet rs = ps.executeQuery();
        
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        
        while (rs.next()) {
            if (!first) json.append(",");
            first = false;
            
            String province = rs.getString("province");
            String district = rs.getString("district");
            String municipal = rs.getString("municipal");
            int wardNum = rs.getInt("ward_num");
            
            String address = buildAddress(province, district, municipal, wardNum);
            
            json.append("{")
                .append("\"id\":").append(rs.getInt("request_id")).append(",")
                .append("\"post_id\":").append(rs.getInt("post_id")).append(",")
                .append("\"owner_id\":").append(rs.getInt("owner_id")).append(",")
                .append("\"status\":\"").append(escapeJson(rs.getString("status"))).append("\",")
                .append("\"type\":\"").append(escapeJson(rs.getString("notification_type"))).append("\",")
                .append("\"title\":\"").append(escapeJson(rs.getString("notification_title"))).append("\",")
                .append("\"message\":\"").append(escapeJson(rs.getString("notification_message"))).append("\",")
                .append("\"post_title\":\"").append(escapeJson(rs.getString("post_title"))).append("\",")
                .append("\"category\":\"").append(escapeJson(rs.getString("category"))).append("\",")
                .append("\"price\":").append(rs.getDouble("price")).append(",")
                .append("\"location\":\"").append(escapeJson(rs.getString("location"))).append("\",")
                .append("\"owner_name\":\"").append(escapeJson(rs.getString("owner_name"))).append("\",")
                .append("\"owner_email\":\"").append(escapeJson(rs.getString("owner_email"))).append("\",")
                .append("\"owner_phone\":\"").append(escapeJson(rs.getString("owner_phone"))).append("\",")
                .append("\"owner_address\":\"").append(escapeJson(address)).append("\",")
                .append("\"request_message\":\"").append(escapeJson(rs.getString("message"))).append("\",")
                .append("\"created_at\":\"").append(rs.getTimestamp("created_at")).append("\"")
                .append("}");
        }
        
        json.append("]");
        response.getWriter().write(json.toString());
    }
    
    private String buildAddress(String province, String district, String municipal, int wardNum) {
        StringBuilder address = new StringBuilder();
        
        if (municipal != null && !municipal.isEmpty()) {
            address.append(municipal);
        }
        
        if (wardNum > 0) {
            if (address.length() > 0) address.append(", ");
            address.append("Ward ").append(wardNum);
        }
        
        if (district != null && !district.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(district);
        }
        
        if (province != null && !province.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(province);
        }
        
        return address.length() > 0 ? address.toString() : "Address not specified";
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}