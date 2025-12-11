package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

@WebServlet("/renterRequests")
public class RenterRequestsServlet extends HttpServlet {
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
        
        System.out.println("\n=== RenterRequestsServlet (GET) ===");
        
        // Check session
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            out.write("{\"error\":\"Please login first\"}");
            return;
        }
        
        int userId = (int) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("role");
        
        // Only renters can access this
        if (!"renter".equals(userRole)) {
            out.write("{\"error\":\"Only renters can access this page\"}");
            return;
        }
        
        String action = request.getParameter("action");
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            
            if ("count".equals(action)) {
                getRenterRequestCounts(userId, conn, out);
            } else {
                getRenterRequests(userId, conn, out);
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            out.write("{\"error\":\"Server error: " + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    private void getRenterRequests(int renterId, Connection conn, PrintWriter out) 
            throws SQLException {
        
        System.out.println("Getting renter requests for ID: " + renterId);
        
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT rr.id, rr.post_id, rr.renter_id, rr.owner_id, rr.message, " +
                    "rr.status, rr.created_at, " +
                    "p.title AS post_title, p.category, p.price, p.location, p.description, " +
                    "u.name AS owner_name, u.email AS owner_email, u.phone AS owner_phone " +
                    "FROM rent_requests rr " +
                    "JOIN posts p ON rr.post_id = p.id " +
                    "JOIN users u ON rr.owner_id = u.id " +
                    "WHERE rr.renter_id = ? " +
                    "ORDER BY rr.created_at DESC";
        
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, renterId);
        
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
                .append("\"description\":\"").append(escapeJson(rs.getString("description"))).append("\",")
                .append("\"owner_name\":\"").append(escapeJson(rs.getString("owner_name"))).append("\",")
                .append("\"owner_email\":\"").append(escapeJson(rs.getString("owner_email"))).append("\",")
                .append("\"owner_phone\":\"").append(escapeJson(rs.getString("owner_phone"))).append("\"")
                .append("}");
        }
        
        json.append("]");
        
        System.out.println("Found " + count + " requests for renter ID " + renterId);
        
        if (count == 0) {
            System.out.println("No requests found for renter");
        }
        
        out.write(json.toString());
        
        rs.close();
        ps.close();
    }
    
    private void getRenterRequestCounts(int renterId, Connection conn, PrintWriter out) 
            throws SQLException {
        
        System.out.println("Getting request counts for renter ID: " + renterId);
        
        StringBuilder json = new StringBuilder("{");
        
        // Total count
        String sqlTotal = "SELECT COUNT(*) as total FROM rent_requests WHERE renter_id = ?";
        PreparedStatement psTotal = conn.prepareStatement(sqlTotal);
        psTotal.setInt(1, renterId);
        ResultSet rsTotal = psTotal.executeQuery();
        int total = 0;
        if (rsTotal.next()) total = rsTotal.getInt("total");
        
        // Pending count
        String sqlPending = "SELECT COUNT(*) as pending FROM rent_requests WHERE renter_id = ? AND status = 'pending'";
        PreparedStatement psPending = conn.prepareStatement(sqlPending);
        psPending.setInt(1, renterId);
        ResultSet rsPending = psPending.executeQuery();
        int pending = 0;
        if (rsPending.next()) pending = rsPending.getInt("pending");
        
        // Approved count
        String sqlApproved = "SELECT COUNT(*) as approved FROM rent_requests WHERE renter_id = ? AND status = 'approved'";
        PreparedStatement psApproved = conn.prepareStatement(sqlApproved);
        psApproved.setInt(1, renterId);
        ResultSet rsApproved = psApproved.executeQuery();
        int approved = 0;
        if (rsApproved.next()) approved = rsApproved.getInt("approved");
        
        // Rejected count
        String sqlRejected = "SELECT COUNT(*) as rejected FROM rent_requests WHERE renter_id = ? AND status = 'rejected'";
        PreparedStatement psRejected = conn.prepareStatement(sqlRejected);
        psRejected.setInt(1, renterId);
        ResultSet rsRejected = psRejected.executeQuery();
        int rejected = 0;
        if (rsRejected.next()) rejected = rsRejected.getInt("rejected");
        
        // Cancelled count
        String sqlCancelled = "SELECT COUNT(*) as cancelled FROM rent_requests WHERE renter_id = ? AND status = 'cancelled'";
        PreparedStatement psCancelled = conn.prepareStatement(sqlCancelled);
        psCancelled.setInt(1, renterId);
        ResultSet rsCancelled = psCancelled.executeQuery();
        int cancelled = 0;
        if (rsCancelled.next()) cancelled = rsCancelled.getInt("cancelled");
        
        json.append("\"total\":").append(total).append(",")
            .append("\"pending\":").append(pending).append(",")
            .append("\"approved\":").append(approved).append(",")
            .append("\"rejected\":").append(rejected).append(",")
            .append("\"cancelled\":").append(cancelled)
            .append("}");
        
        System.out.println("Counts for renter " + renterId + ": " + json.toString());
        out.write(json.toString());
        
        // Clean up
        rsTotal.close(); rsPending.close(); rsApproved.close(); 
        rsRejected.close(); rsCancelled.close();
        psTotal.close(); psPending.close(); psApproved.close();
        psRejected.close(); psCancelled.close();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        System.out.println("\n=== RenterRequestsServlet (POST) ===");
        
        // Check session
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            out.write("{\"success\":false,\"message\":\"Please login first\"}");
            return;
        }
        
        int userId = (int) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("role");
        
        // Only renters can access this
        if (!"renter".equals(userRole)) {
            out.write("{\"success\":false,\"message\":\"Only renters can perform this action\"}");
            return;
        }
        
        String action = request.getParameter("action");
        String requestIdStr = request.getParameter("requestId");
        
        if ("cancel".equals(action)) {
            cancelRenterRequest(userId, requestIdStr, out);
        } else {
            out.write("{\"success\":false,\"message\":\"Invalid action\"}");
        }
    }
    
    private void cancelRenterRequest(int renterId, String requestIdStr, PrintWriter out) {
        
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
            psVerify.setInt(2, renterId);
            ResultSet rsVerify = psVerify.executeQuery();
            
            if (!rsVerify.next()) {
                System.out.println("Cannot cancel request: ID=" + requestId + ", RenterID=" + renterId);
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
            
        } catch (Exception e) {
            System.out.println("Error cancelling request: " + e.getMessage());
            out.write("{\"success\":false,\"message\":\"Server error: " + escapeJson(e.getMessage()) + "\"}");
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