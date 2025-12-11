package com.rentalapp.servlets;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/testSession")
public class TestSessionServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        
        out.write("{");
        out.write("\"sessionId\":\"" + (session != null ? session.getId() : "null") + "\",");
        out.write("\"userId\":\"" + (session != null ? session.getAttribute("userId") : "null") + "\",");
        out.write("\"username\":\"" + (session != null ? session.getAttribute("username") : "null") + "\",");
        out.write("\"newSession\":\"" + (request.getSession(false) == null ? "true" : "false") + "\"");
        out.write("}");
    }
}