package app;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

@WebServlet("/matchedLogs")
public class MatchedLogsServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!SessionUtils.checkLogin(req, resp)) {
			return;
		}
        RequestDispatcher rd = req.getRequestDispatcher("matchedLogs.jsp");
        rd.forward(req, resp);
    }
}
