package com.rentalapp.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import java.io.*;
import java.util.Enumeration;

@WebServlet("/sessionDebug")
public class SessionDebugServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        
        PrintWriter out = response.getWriter();
        StringBuilder json = new StringBuilder();
        
        json.append("{\n");
        
        // Check session
        HttpSession session = request.getSession(false);
        if (session == null) {
            json.append("  \"hasSession\": false,\n");
            json.append("  \"message\": \"No active session found\",\n");
            json.append("  \"requestedSessionId\": \"").append(request.getRequestedSessionId()).append("\",\n");
            json.append("  \"isRequestedSessionIdValid\": ").append(request.isRequestedSessionIdValid()).append("\n");
        } else {
            json.append("  \"hasSession\": true,\n");
            json.append("  \"sessionId\": \"").append(session.getId()).append("\",\n");
            json.append("  \"creationTime\": \"").append(new java.util.Date(session.getCreationTime())).append("\",\n");
            json.append("  \"lastAccessedTime\": \"").append(new java.util.Date(session.getLastAccessedTime())).append("\",\n");
            json.append("  \"maxInactiveInterval\": ").append(session.getMaxInactiveInterval()).append(",\n");
            json.append("  \"isNew\": ").append(session.isNew()).append(",\n");
            
            // Session attributes
            json.append("  \"attributes\": {\n");
            Enumeration<String> attrNames = session.getAttributeNames();
            boolean firstAttr = true;
            while (attrNames.hasMoreElements()) {
                if (!firstAttr) json.append(",\n");
                firstAttr = false;
                
                String name = attrNames.nextElement();
                Object value = session.getAttribute(name);
                json.append("    \"").append(name).append("\": \"").append(value).append("\"");
            }
            json.append("\n  },\n");
            
            // Cookies
            json.append("  \"cookies\": [\n");
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (int i = 0; i < cookies.length; i++) {
                    if (i > 0) json.append(",\n");
                    json.append("    {\n");
                    json.append("      \"name\": \"").append(cookies[i].getName()).append("\",\n");
                    json.append("      \"value\": \"").append(cookies[i].getValue()).append("\",\n");
                    json.append("      \"domain\": \"").append(cookies[i].getDomain()).append("\",\n");
                    json.append("      \"path\": \"").append(cookies[i].getPath()).append("\",\n");
                    json.append("      \"maxAge\": ").append(cookies[i].getMaxAge()).append("\n");
                    json.append("    }");
                }
            }
            json.append("\n  ]\n");
        }
        
        json.append("}");
        out.write(json.toString());
    }
}