package com.rentalapp.servlets;

import java.io.IOException;
import java.sql.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String URL = "jdbc:mysql://localhost:3306/rental_project";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("username");
        String password = request.getParameter("password");

        System.out.println("=== Login Attempt ===");
        System.out.println("Email: " + email);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

            String sql = "SELECT id, name, email, phone, role, province, district, municipal, ward_num "
                       + "FROM users WHERE email=? AND password=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("✓ User found in database");

                // Invalidate any existing session
                HttpSession oldSession = request.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }

                // Create new session
                HttpSession session = request.getSession(true);
                
                // Set session attributes
                session.setAttribute("userId", rs.getInt("id"));
                session.setAttribute("username", email);
                session.setAttribute("role", rs.getString("role"));
                session.setAttribute("name", rs.getString("name"));
                session.setAttribute("phone", rs.getString("phone"));
                session.setAttribute("province", rs.getString("province"));
                session.setAttribute("district", rs.getString("district"));
                session.setAttribute("municipal", rs.getString("municipal"));
                session.setAttribute("ward_num", rs.getInt("ward_num"));

                // Set session timeout to 30 minutes
                session.setMaxInactiveInterval(30 * 60);
                
                System.out.println("Session created with ID: " + session.getId());
                System.out.println("User ID stored: " + session.getAttribute("userId"));

                // Set session cookie explicitly
                Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
                sessionCookie.setPath("/RentalApp/");
                sessionCookie.setMaxAge(30 * 60);
                response.addCookie(sessionCookie);

                // Redirect based on role
                String role = rs.getString("role");
                if ("owner".equalsIgnoreCase(role)) {
                    response.sendRedirect("owner.html");
                } else {
                    response.sendRedirect("main.html");
                }

            } else {
                System.out.println("✗ Invalid credentials");
                response.sendRedirect("login.html?error=invalid");
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception: " + e.getMessage());
            response.sendRedirect("login.html?error=exception");
        }
    }
}