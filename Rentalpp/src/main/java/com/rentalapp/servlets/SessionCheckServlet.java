package com.rentalapp.servlets;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/checkSession")
public class SessionCheckServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        
        if (session != null && session.getAttribute("userId") != null) {
            System.out.println("Session found - User ID: " + session.getAttribute("userId"));
            out.write("{\"hasSession\":true,\"userId\":" + session.getAttribute("userId") + 
                     ",\"username\":\"" + session.getAttribute("username") + "\"}");
        } else {
            System.out.println("No valid session found");
            out.write("{\"hasSession\":false}");
        }
    }
}