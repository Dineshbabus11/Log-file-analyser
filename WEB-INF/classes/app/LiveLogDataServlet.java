package app;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import com.google.gson.Gson;

@WebServlet("/liveLogData")
public class LiveLogDataServlet extends HttpServlet {
    private Gson gson = new Gson();

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String indexName = req.getParameter("indexName");
        if (indexName == null || indexName.trim().isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        RestHighLevelClient client = ESClient.getClient();
        boolean exists = client.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);
        if (!exists) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Index not found: " + indexName + "\"}");
            return;
        }
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(new MatchAllQueryBuilder());
        sourceBuilder.size(50);
        searchRequest.source(sourceBuilder);
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        List<Map<String,Object>> logList = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            logList.add(hit.getSourceAsMap());
        }
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        out.print(gson.toJson(logList));
    }
}
