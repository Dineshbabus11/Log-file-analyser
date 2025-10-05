package app;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.CreateIndexRequest;

@WebServlet("/addApiUrl")
public class AddApiUrlServlet extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String apiUrl = req.getParameter("newApiUrl").trim();
        String apiKey = req.getParameter("apiKey").trim();

        if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            res.sendRedirect("AddApiUrl.jsp");
            return;
        }

        String indexName = "api_" + apiUrl.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();

        try (Connection con = DBconnect.connect()) {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO watched_apis(api_url, api_key, index_name, enabled) VALUES (?, ?, ?, TRUE) " +
                "ON CONFLICT(api_url) DO UPDATE SET enabled=TRUE, api_key=excluded.api_key");
            ps.setString(1, apiUrl);
            ps.setString(2, apiKey);
            ps.setString(3, indexName);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            RestHighLevelClient client = ESClient.getClient();
            client.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
        } catch (Exception ignore) {}

        try {
            RestHighLevelClient client = ESClient.getClient();
            client.indices().create(new CreateIndexRequest(indexName), RequestOptions.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ApiWatcherManager.startTracking(apiUrl, apiKey, indexName);

        res.sendRedirect("manageApiUrls.jsp");
    }
}
