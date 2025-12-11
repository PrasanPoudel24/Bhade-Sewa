package com.rentalapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; // ✅ ADD THIS IMPORT

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String USER = "root";     
    private static final String PASSWORD = "";     

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // Get form data
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String role = request.getParameter("role");
        String province = request.getParameter("province");
        String district = request.getParameter("district");
        String municipal = request.getParameter("municipal");
        
        int ward_num = 0;
        try {
            ward_num = Integer.parseInt(request.getParameter("ward_num"));
        } catch (NumberFormatException e) {
            ward_num = 0;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement checkPs = null; // ✅ FIXED: Separate statement for checking email
        ResultSet rs = null;
        ResultSet generatedKeys = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            
            String checkSql = "SELECT id FROM users WHERE phone = ?";
            checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, phone);
            rs = checkPs.executeQuery();
            if (rs.next()) {
                out.println("<script>alert('Phone number already registered! Please use a different phone number.'); window.location='register.html';</script>");
                return;
            }

            // First check if email already exists
             checkSql = "SELECT id FROM users WHERE email = ?";
            checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, email);
            rs = checkPs.executeQuery();
            
            if (rs.next()) {
                out.println("<script>alert('Email already registered! Please use a different email.'); window.location='register.html';</script>");
                return;
            }

            // Insert query with RETURN_GENERATED_KEYS
            String sql = "INSERT INTO users (name, email, phone, password, role, province, district, municipal, ward_num) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS); // ✅ FIXED: Now Statement is imported

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, password);
            ps.setString(5, role);
            ps.setString(6, province);
            ps.setString(7, district);
            ps.setString(8, municipal);
            ps.setInt(9, ward_num);

            int result = ps.executeUpdate();

            if (result > 0) {
                // Get the generated user ID
                generatedKeys = ps.getGeneratedKeys();
                int userId = 0;
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                }
                
                // Create session for auto-login
                HttpSession session = request.getSession();
                session.setAttribute("userId", userId);
                session.setAttribute("username", email);
                session.setAttribute("name", name);
                session.setAttribute("role", role);
                session.setAttribute("phone", phone);
                session.setAttribute("province", province);
                session.setAttribute("district", district);
                session.setAttribute("municipal", municipal);
                session.setAttribute("ward_num", ward_num);
                session.setMaxInactiveInterval(30 * 60);
                
                // Redirect based on role
                if ("owner".equalsIgnoreCase(role)) {
                    out.println("<script>alert('Registration successful!'); window.location='owner.html';</script>");
                } else {
                    out.println("<script>alert('Registration successful!'); window.location='main.html';</script>");
                }
            } else {
                out.println("<script>alert('Registration failed. Try again.'); window.location='register.html';</script>");
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            out.println("<h3>Error: MySQL Driver not found!</h3>");
        } catch (SQLException e) {
            e.printStackTrace();
            out.println("<h3>Error: Database issue occurred!</h3>");
        } finally {
            // Close all resources properly
            try {
                if (generatedKeys != null) generatedKeys.close();
                if (rs != null) rs.close();
                if (checkPs != null) checkPs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}