package app;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/deleteToken")
public class DeleteTokenServlet extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        String tokenIdStr = req.getParameter("tokenId");
        try (Connection con = DBconnect.connect()) {
            PreparedStatement delete = con.prepareStatement("DELETE FROM index_tokens WHERE token_id = ?");
            delete.setInt(1, Integer.parseInt(tokenIdStr));
            delete.executeUpdate();
        } catch (Exception e) {
            throw new ServletException("Error deleting token", e);
        }
        res.sendRedirect("manageindextracking.jsp");
    }
}
