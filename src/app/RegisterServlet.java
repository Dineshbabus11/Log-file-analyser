package app;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("register.jsp").forward(req, resp);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username").trim();
        String password = req.getParameter("password").trim();
        String role = req.getParameter("role").trim();

        try (Connection con = DBconnect.connect();
             PreparedStatement ps = con.prepareStatement("INSERT INTO users (username, password, role) VALUES (?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.executeUpdate();
            resp.sendRedirect("login.jsp");
        } 
		catch (SQLException e) {
            req.setAttribute("error", "User already exists or registration error.");
            req.getRequestDispatcher("register.jsp").forward(req, resp);
        }
    }
}
