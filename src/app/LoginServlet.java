package app;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.getRequestDispatcher("login.jsp").forward(req, res);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String username = req.getParameter("username").trim();
        String password = req.getParameter("password").trim();

        try (Connection con = DBconnect.connect();
             PreparedStatement ps = con.prepareStatement("SELECT role FROM users WHERE username = ? AND password = ?")) {
            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role").trim();
                    if ("admin".equals(role)) {
                        HttpSession session = req.getSession();
                        session.setAttribute("username", username);
                        session.setAttribute("role", role);
                        session.setMaxInactiveInterval(3600);
                        res.sendRedirect("index.jsp");
                        return;
                    }
                }
            }
        } 
		catch (Exception e) {
            e.printStackTrace();
        }

        req.setAttribute("error", "Invalid credentials or not admin user.");
        req.getRequestDispatcher("login.jsp").forward(req, res);
    }
}
