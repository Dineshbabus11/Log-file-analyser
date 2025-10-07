package app;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/viewIndex")
public class ViewIndexPageServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!SessionUtils.checkLogin(req, res)) {
            return;
        }
        String indexName = req.getParameter("indexName");
        if (indexName == null || indexName.trim().isEmpty()) {
            res.sendRedirect("manageindextracking.jsp");
            return;
        }
        req.setAttribute("indexName", indexName);
        req.getRequestDispatcher("viewIndex.jsp").forward(req, res);
    }
}
