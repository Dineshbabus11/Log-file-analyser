package app;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.util.*;

@WebServlet("/matchedLogsJson")
public class MatchedLogsJsonServlet extends HttpServlet {
    private Gson gson = new Gson();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String lastIdParam = req.getParameter("lastId");
        int lastId = 0;
        if (lastIdParam != null) {
            try {
                lastId = Integer.parseInt(lastIdParam);
            } catch (NumberFormatException e) {
                lastId = 0;
            }
        }

        List<Map<String,Object>> logs=new ArrayList<>();

        try {
            RestHighLevelClient client = ESClient.getClient();
            SearchRequest searchRequest = new SearchRequest("logs");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.rangeQuery("id").gt(lastId));
            sourceBuilder.size(100);
            searchRequest.source(sourceBuilder);
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            for (SearchHit hit : searchResponse.getHits()) {
                logs.add(hit.getSourceAsMap());
            }
        } 
		catch (Exception e) {
            e.printStackTrace();
        }
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(logs));
            out.flush();
        }
    }
}
