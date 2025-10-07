package app;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;

@WebServlet("/addMultiAppPath")
public class AddMultiAppPathServlet extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		if (!SessionUtils.checkLogin(req, res)) {
			return;
		}
        String newPath=req.getParameter("newPath").trim();
        if (newPath==null || newPath.isEmpty()) {
            res.sendRedirect("MultiApp.jsp");
            return;
        }
        String indexName="path_"+newPath.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        RestHighLevelClient client=ESClient.getClient();
        try {
            client.indices().delete(new DeleteIndexRequest(indexName),RequestOptions.DEFAULT);
        } 
		catch (Exception e) {
            e.printStackTrace();
        }
        DeleteByQueryRequest delReq=new DeleteByQueryRequest("paths");
        delReq.setQuery(QueryBuilders.termQuery("pathString.keyword",newPath));
        client.deleteByQuery(delReq,RequestOptions.DEFAULT);
        try {
            client.indices().create(new CreateIndexRequest(indexName),RequestOptions.DEFAULT);
        } 
		catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, Object> pathDoc = new HashMap<>();
        pathDoc.put("pathString", newPath);
        client.index(new IndexRequest("paths").source(pathDoc, XContentType.JSON), RequestOptions.DEFAULT);
        MultiAppPathWatcherManager.startTracking(newPath, indexName);
        res.sendRedirect("manageMultiAppPaths.jsp");
    }
}
