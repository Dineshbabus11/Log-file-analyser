package app;

import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;

@WebServlet("/viewRules")
public class ViewRules extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        try (Connection con = DBconnect.connect();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM rules");
             ResultSet rs = ps.executeQuery()) {

            req.setAttribute("rulesResultSet", rs);
            RequestDispatcher rd = req.getRequestDispatcher("viewRules.jsp");
            rd.forward(req, res);

        } 
		catch (Exception e) {
            e.printStackTrace();
        }
    }
}
