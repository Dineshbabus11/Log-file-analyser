package app;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;

@WebServlet("/addIndex")
public class AddIndexServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String indexName = req.getParameter("indexName").trim();

        try (Connection con = DBconnect.connect()) {
            PreparedStatement checkStmt = con.prepareStatement("SELECT 1 FROM tracked_indices WHERE index_name = ?");
            checkStmt.setString(1, indexName);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                req.setAttribute("warningMessage", "Index '" + indexName + "' is already tracked.");
                req.getRequestDispatcher("addIndex.jsp").forward(req, res);
                return;
            }

            RestHighLevelClient client = ESClient.getClient();
            boolean exists = client.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);
            if (!exists) {
                CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
                client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            }

            PreparedStatement insertStmt = con.prepareStatement("INSERT INTO tracked_indices(index_name, enabled) VALUES (?, TRUE)");
            insertStmt.setString(1, indexName);
            insertStmt.executeUpdate();

        } catch (Exception e) {
            throw new ServletException("Error adding index", e);
        }
        res.sendRedirect("manageindextracking.jsp");
    }
}
