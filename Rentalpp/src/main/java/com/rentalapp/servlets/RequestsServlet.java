package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/requests")
public class RequestsServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        
        PrintWriter out = response.getWriter();
        
        System.out.println("\n=== RequestsServlet (GET) ===");
        System.out.println("Request URL: " + request.getRequestURL());
        System.out.println("Query String: " + request.getQueryString());
        
        // Check session
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            System.out.println("No session or user ID found");
            out.write("{\"error\":\"Please login first\"}");
            return;
        }
        
        int userId = (int) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("role");
        String userName = (String) session.getAttribute("name");
        
        System.out.println("User - ID: " + userId + ", Name: " + userName + ", Role: " + userRole);
        
        String action = request.getParameter("action");
        System.out.println("Action parameter: " + action);
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            
            if ("count".equals(action)) {
                System.out.println("Getting request counts...");
                getRequestCounts(userId, userRole, conn, out);
            } else {
                System.out.println("Getting requests list...");
                getRequestsList(userId, userRole, conn, out);
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            out.write("{\"error\":\"Server error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    private void getRequestsList(int userId, String userRole, Connection conn, PrintWriter out) 
            throws SQLException {
        
        System.out.println("Getting requests list for user ID: " + userId + ", Role: " + userRole);
        
        StringBuilder json = new StringBuilder("[");
        String sql;
        PreparedStatement ps;
        
        if ("owner".equals(userRole)) {
            // Owner: see requests received for their properties
            sql = "SELECT rr.id, rr.post_id, rr.renter_id, rr.owner_id, rr.message, " +
                  "rr.status, rr.created_at, " +
                  "p.title AS post_title, p.category, p.price, p.location, p.description, " +
                  "u.name AS renter_name, u.email AS renter_email, u.phone AS renter_phone " +
                  "FROM rent_requests rr " +
                  "JOIN posts p ON rr.post_id = p.id " +
                  "JOIN users u ON rr.renter_id = u.id " +
                  "WHERE rr.owner_id = ? " +
                  "ORDER BY rr.created_at DESC";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            System.out.println("Executing SQL for owner: " + sql);
        } else {
            // Renter: see requests they sent
            sql = "SELECT rr.id, rr.post_id, rr.renter_id, rr.owner_id, rr.message, " +
                  "rr.status, rr.created_at, " +
                  "p.title AS post_title, p.category, p.price, p.location, p.description, " +
                  "u.name AS owner_name, u.email AS owner_email, u.phone AS owner_phone " +
                  "FROM rent_requests rr " +
                  "JOIN posts p ON rr.post_id = p.id " +
                  "JOIN users u ON rr.owner_id = u.id " +
                  "WHERE rr.renter_id = ? " +
                  "ORDER BY rr.created_at DESC";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            System.out.println("Executing SQL for renter: " + sql);
        }
        
        ResultSet rs = ps.executeQuery();
        boolean first = true;
        int count = 0;
        
        while (rs.next()) {
            count++;
            if (!first) json.append(",");
            first = false;
            
            json.append("{")
                .append("\"id\":").append(rs.getInt("id")).append(",")
                .append("\"post_id\":").append(rs.getInt("post_id")).append(",")
                .append("\"renter_id\":").append(rs.getInt("renter_id")).append(",")
                .append("\"owner_id\":").append(rs.getInt("owner_id")).append(",")
                .append("\"message\":\"").append(escapeJson(rs.getString("message"))).append("\",")
                .append("\"status\":\"").append(rs.getString("status")).append("\",")
                .append("\"created_at\":\"").append(rs.getTimestamp("created_at")).append("\",")
                .append("\"post_title\":\"").append(escapeJson(rs.getString("post_title"))).append("\",")
                .append("\"category\":\"").append(escapeJson(rs.getString("category"))).append("\",")
                .append("\"price\":").append(rs.getDouble("price")).append(",")
                .append("\"location\":\"").append(escapeJson(rs.getString("location"))).append("\",")
                .append("\"description\":\"").append(escapeJson(rs.getString("description"))).append("\",");
            
            if ("owner".equals(userRole)) {
                json.append("\"renter_name\":\"").append(escapeJson(rs.getString("renter_name"))).append("\",")
                    .append("\"renter_email\":\"").append(escapeJson(rs.getString("renter_email"))).append("\",")
                    .append("\"renter_phone\":\"").append(escapeJson(rs.getString("renter_phone"))).append("\",")
                    .append("\"user_type\":\"owner\"");
            } else {
                json.append("\"owner_name\":\"").append(escapeJson(rs.getString("owner_name"))).append("\",")
                    .append("\"owner_email\":\"").append(escapeJson(rs.getString("owner_email"))).append("\",")
                    .append("\"owner_phone\":\"").append(escapeJson(rs.getString("owner_phone"))).append("\",")
                    .append("\"user_type\":\"renter\"");
            }
            
            json.append("}");
        }
        
        json.append("]");
        
        System.out.println("Found " + count + " requests for user ID " + userId);
        System.out.println("Returning JSON length: " + json.length() + " characters");
        
        if (count == 0) {
            System.out.println("No requests found, returning empty array");
        }
        
        out.write(json.toString());
        
        // Clean up
        rs.close();
        ps.close();
    }
    
    private void getRequestCounts(int userId, String userRole, Connection conn, PrintWriter out) 
            throws SQLException {
        
        System.out.println("Getting request counts for user ID: " + userId + ", Role: " + userRole);
        
        StringBuilder json = new StringBuilder("{");
        String whereClause = "owner".equals(userRole) ? "WHERE owner_id = ?" : "WHERE renter_id = ?";
        
        // Total count
        String sqlTotal = "SELECT COUNT(*) as total FROM rent_requests " + whereClause;
        PreparedStatement psTotal = conn.prepareStatement(sqlTotal);
        psTotal.setInt(1, userId);
        ResultSet rsTotal = psTotal.executeQuery();
        int total = 0;
        if (rsTotal.next()) total = rsTotal.getInt("total");
        
        // Pending count
        String sqlPending = "SELECT COUNT(*) as pending FROM rent_requests " + whereClause + " AND status = 'pending'";
        PreparedStatement psPending = conn.prepareStatement(sqlPending);
        psPending.setInt(1, userId);
        ResultSet rsPending = psPending.executeQuery();
        int pending = 0;
        if (rsPending.next()) pending = rsPending.getInt("pending");
        
        // Approved count
        String sqlApproved = "SELECT COUNT(*) as approved FROM rent_requests " + whereClause + " AND status = 'approved'";
        PreparedStatement psApproved = conn.prepareStatement(sqlApproved);
        psApproved.setInt(1, userId);
        ResultSet rsApproved = psApproved.executeQuery();
        int approved = 0;
        if (rsApproved.next()) approved = rsApproved.getInt("approved");
        
        // Rejected count
        String sqlRejected = "SELECT COUNT(*) as rejected FROM rent_requests " + whereClause + " AND status = 'rejected'";
        PreparedStatement psRejected = conn.prepareStatement(sqlRejected);
        psRejected.setInt(1, userId);
        ResultSet rsRejected = psRejected.executeQuery();
        int rejected = 0;
        if (rsRejected.next()) rejected = rsRejected.getInt("rejected");
        
        json.append("\"total\":").append(total).append(",")
            .append("\"pending\":").append(pending).append(",")
            .append("\"approved\":").append(approved).append(",")
            .append("\"rejected\":").append(rejected);
        
        // Add cancelled count for renters
        if (!"owner".equals(userRole)) {
            String sqlCancelled = "SELECT COUNT(*) as cancelled FROM rent_requests " + whereClause + " AND status = 'cancelled'";
            PreparedStatement psCancelled = conn.prepareStatement(sqlCancelled);
            psCancelled.setInt(1, userId);
            ResultSet rsCancelled = psCancelled.executeQuery();
            int cancelled = 0;
            if (rsCancelled.next()) cancelled = rsCancelled.getInt("cancelled");
            json.append(",\"cancelled\":").append(cancelled);
        }
        
        json.append("}");
        
        System.out.println("Counts for user " + userId + ": " + json.toString());
        out.write(json.toString());
        
        // Clean up
        rsTotal.close();
        rsPending.close();
        rsApproved.close();
        rsRejected.close();
        psTotal.close();
        psPending.close();
        psApproved.close();
        psRejected.close();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        System.out.println("\n=== RequestsServlet (POST) ===");
        
        // Check session
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            out.write("{\"success\":false,\"message\":\"Please login first\"}");
            return;
        }
        
        int userId = (int) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("role");
        String userName = (String) session.getAttribute("name");
        
        System.out.println("User - ID: " + userId + ", Name: " + userName + ", Role: " + userRole);
        
        String action = request.getParameter("action");
        String requestIdStr = request.getParameter("requestId");
        String status = request.getParameter("status");
        
        System.out.println("Action: " + action + ", Request ID: " + requestIdStr + ", Status: " + status);
        
        try {
            if ("update".equals(action)) {
                updateRequestStatus(userId, userRole, requestIdStr, status, out);
            } else if ("cancel".equals(action)) {
                cancelRenterRequest(userId, requestIdStr, out);
            } else {
                out.write("{\"success\":false,\"message\":\"Invalid action\"}");
            }
        } catch (Exception e) {
            System.out.println("Error in POST: " + e.getMessage());
            e.printStackTrace();
            out.write("{\"success\":false,\"message\":\"Server error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    private void updateRequestStatus(int userId, String userRole, String requestIdStr, String status, PrintWriter out) 
            throws SQLException {
        
        // Only owners can update request status
        if (!"owner".equals(userRole)) {
            out.write("{\"success\":false,\"message\":\"Only property owners can update request status\"}");
            return;
        }
        
        if (requestIdStr == null || status == null) {
            out.write("{\"success\":false,\"message\":\"Invalid parameters\"}");
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            int requestId = Integer.parseInt(requestIdStr);
            
            // Verify the request belongs to this owner
            String sqlVerify = "SELECT id FROM rent_requests WHERE id = ? AND owner_id = ?";
            PreparedStatement psVerify = conn.prepareStatement(sqlVerify);
            psVerify.setInt(1, requestId);
            psVerify.setInt(2, userId);
            ResultSet rsVerify = psVerify.executeQuery();
            
            if (!rsVerify.next()) {
                System.out.println("Request not found or unauthorized: ID=" + requestId + ", OwnerID=" + userId);
                out.write("{\"success\":false,\"message\":\"Request not found or unauthorized\"}");
                return;
            }
            
            // Update the status
            String sqlUpdate = "UPDATE rent_requests SET status = ? WHERE id = ?";
            PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
            psUpdate.setString(1, status);
            psUpdate.setInt(2, requestId);
            
            int rows = psUpdate.executeUpdate();
            
            if (rows > 0) {
                String actionText = getStatusActionText(status);
                System.out.println("Request " + actionText + " successfully! Rows updated: " + rows);
                out.write("{\"success\":true,\"message\":\"Request " + actionText + " successfully!\"}");
            } else {
                System.out.println("Failed to update request. No rows affected.");
                out.write("{\"success\":false,\"message\":\"Failed to update request\"}");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid request ID format: " + requestIdStr);
            out.write("{\"success\":false,\"message\":\"Invalid request ID format\"}");
        }
    }
    
    private void cancelRenterRequest(int userId, String requestIdStr, PrintWriter out) 
            throws SQLException {
        
        if (requestIdStr == null) {
            out.write("{\"success\":false,\"message\":\"Invalid parameters\"}");
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            int requestId = Integer.parseInt(requestIdStr);
            
            // Verify the request belongs to this renter and is pending
            String sqlVerify = "SELECT id FROM rent_requests WHERE id = ? AND renter_id = ? AND status = 'pending'";
            PreparedStatement psVerify = conn.prepareStatement(sqlVerify);
            psVerify.setInt(1, requestId);
            psVerify.setInt(2, userId);
            ResultSet rsVerify = psVerify.executeQuery();
            
            if (!rsVerify.next()) {
                System.out.println("Cannot cancel request: ID=" + requestId + ", RenterID=" + userId);
                out.write("{\"success\":false,\"message\":\"Cannot cancel request. Either it doesn't exist, you're not the renter, or it's not pending.\"}");
                return;
            }
            
            // Update status to 'cancelled'
            String sqlUpdate = "UPDATE rent_requests SET status = 'cancelled' WHERE id = ?";
            PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
            psUpdate.setInt(1, requestId);
            
            int rows = psUpdate.executeUpdate();
            
            if (rows > 0) {
                System.out.println("Request cancelled successfully! Rows updated: " + rows);
                out.write("{\"success\":true,\"message\":\"Request cancelled successfully!\"}");
            } else {
                System.out.println("Failed to cancel request. No rows affected.");
                out.write("{\"success\":false,\"message\":\"Failed to cancel request\"}");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid request ID format: " + requestIdStr);
            out.write("{\"success\":false,\"message\":\"Invalid request ID format\"}");
        }
    }
    
    private String getStatusActionText(String status) {
        if ("approved".equalsIgnoreCase(status)) {
            return "accepted";
        } else if ("rejected".equalsIgnoreCase(status)) {
            return "rejected";
        } else if ("cancelled".equalsIgnoreCase(status)) {
            return "cancelled";
        } else {
            return "updated";
        }
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }
}