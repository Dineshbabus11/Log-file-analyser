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

@WebServlet("/viewIndexData")
public class ViewIndexServlet extends HttpServlet {

    private Gson gson = new Gson();

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!SessionUtils.checkLogin(req, res)) {
            return;
        }

        String indexName = req.getParameter("indexName");
        int page = 1;
        int size = 50;

        try {
            page = Integer.parseInt(req.getParameter("page"));
            if (page < 1){
                page = 1;
            }
        } catch (Exception ignored) {}

        try {
            size = Integer.parseInt(req.getParameter("size"));
            if (size < 1){
                size = 50;
            }
        } catch (Exception ignored) {}

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
        sourceBuilder.from((page - 1) * size);
        sourceBuilder.size(size);
        searchRequest.source(sourceBuilder);

        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        long totalHits = response.getHits().getTotalHits().value;
        List<Map<String, Object>> logList = new ArrayList<>();
        Set<String> allKeysSet = new LinkedHashSet<>();
        for (SearchHit hit : response.getHits().getHits()) {
            Map<String, Object> source = hit.getSourceAsMap();
            logList.add(source);
            if (source != null) {
                allKeysSet.addAll(source.keySet());
            }
        }
        List<String> allKeys = new ArrayList<>(allKeysSet);
        Collections.sort(allKeys);
        Map<String, Object> result = new HashMap<>();
        result.put("totalHits", totalHits);
        result.put("page", page);
        result.put("size", size);
        result.put("allKeys", allKeys);
        result.put("logs", logList);
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        out.print(gson.toJson(result));
    }
}
