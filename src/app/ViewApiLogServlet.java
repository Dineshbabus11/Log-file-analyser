package app;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.*;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

@WebServlet("/viewApiLog")
public class ViewApiLogServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String indexName=req.getParameter("indexName");
        if(indexName==null || indexName.trim().isEmpty()){
            res.sendRedirect("manageApiUrls.jsp");
            return;
        }
        RestHighLevelClient client=ESClient.getClient();
        SearchRequest searchRequest=new SearchRequest(indexName);
        SearchSourceBuilder sourceBuilder=new SearchSourceBuilder();
        sourceBuilder.query(new MatchAllQueryBuilder());
        sourceBuilder.size(1000);
        searchRequest.source(sourceBuilder);
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        List<Map<String, Object>> logs=new ArrayList<>();
        for (SearchHit hit:response.getHits().getHits()) {
            logs.add(hit.getSourceAsMap());
        }
        req.setAttribute("logs", logs);
        req.setAttribute("apiIndex", indexName);
        req.getRequestDispatcher("viewApiLog.jsp").forward(req, res);
    }
}
