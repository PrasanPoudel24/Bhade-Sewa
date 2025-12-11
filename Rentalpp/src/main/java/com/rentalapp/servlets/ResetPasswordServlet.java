package com.rentalapp.servlets;

import java.io.*;
import java.sql.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/resetPassword")
public class ResetPasswordServlet extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        StringBuilder jsonResponse = new StringBuilder();
        
        try {
            // Read request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            String body = sb.toString();
            String phone = extractValue(body, "phone");
            String email = extractValue(body, "email");
            String newPassword = extractValue(body, "newPassword");
            
            System.out.println("Resetting password for user: " + email);
            
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                
                String sql = "UPDATE users SET password = ? WHERE phone = ? AND email = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, newPassword);
                    ps.setString(2, phone);
                    ps.setString(3, email);
                    
                    int rowsAffected = ps.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        jsonResponse.append("{\"success\":true,\"message\":\"Password updated successfully\"}");
                        System.out.println("Password updated for user: " + email);
                    } else {
                        jsonResponse.append("{\"success\":false,\"message\":\"User not found or verification failed\"}");
                        System.out.println("Password update failed - user not found");
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.append("{\"success\":false,\"error\":\"").append(e.getMessage()).append("\"}");
        }
        
        out.print(jsonResponse.toString());
        out.flush();
    }
    
    private String extractValue(String json, String key) {
        try {
            int start = json.indexOf("\"" + key + "\"") + key.length() + 3;
            int end = json.indexOf("\"", start);
            if (end == -1) {
                end = json.indexOf(",", start);
                if (end == -1) end = json.indexOf("}", start);
            }
            return json.substring(start, end).replace("\"", "");
        } catch (Exception e) {
            return "";
        }
    }
}