package app;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/controlMultiAppPath")
public class ControlMultiAppPathServlet extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		SessionUtils.setNoCacheHeaders(res);
		if (!SessionUtils.checkLogin(req, res)) {
			return;
		}
        String path = req.getParameter("path");
        String action = req.getParameter("action");

        if(path == null || action == null){
            res.sendRedirect("manageMultiAppPaths.jsp");
            return;
        }

        if("stop".equalsIgnoreCase(action)){
            MultiAppPathWatcherManager.stopTracking(path);
        } 
		else if ("start".equalsIgnoreCase(action)) {
            String indexName = "path_" + path.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
            MultiAppPathWatcherManager.startTracking(path, indexName);
        }

        res.sendRedirect("manageMultiAppPaths.jsp");
    }
}
