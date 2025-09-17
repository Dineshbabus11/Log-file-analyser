package app;

import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;

//javac -cp "C:\Users\Arul selvan\OneDrive\Desktop\Dinesh\Zoho Official\apache-tomcat-9.0.109\lib\*;." *.java
@WebServlet("/filterMessage")
public class FilterMessage extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String msgPattern = req.getParameter("msgPattern");
        String fileName = req.getParameter("fileName");
        try (Connection con = DBconnect.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT log_time, log_date, logger, level, code, message FROM log WHERE filename = ? AND message ~ ?")) {
            ps.setString(1, fileName);
            ps.setString(2, msgPattern);
            try (ResultSet rs = ps.executeQuery()) {
                req.setAttribute("fileName", fileName);
                req.setAttribute("resultSet", rs);
                RequestDispatcher rd = req.getRequestDispatcher("results.jsp");
                rd.forward(req, res);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
