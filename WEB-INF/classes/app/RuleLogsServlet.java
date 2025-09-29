package app;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.util.*;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.action.search.SearchResponse;

@WebServlet("/ruleLogs")
public class RuleLogsServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RestHighLevelClient client = ESClient.getClient();
        Map<Integer, List<LogEntry>> logsByRuleId = new LinkedHashMap<>();
        Map<Integer, String> ruleIdNameMap = new HashMap<>();
        int defaultPageSize = 10;
        SearchRequest searchRequest = new SearchRequest("logs");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        sourceBuilder.size(1000);
        searchRequest.source(sourceBuilder);
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);
        for (SearchHit hit:response.getHits().getHits()) {
            Map<String,Object> source = hit.getSourceAsMap();
            String time=(String) source.get("time");
            String date=(String) source.get("date");
            String logger=(String) source.get("logger");
            String level=(String) source.get("level");
            String code=(String) source.get("code");
            String message=(String) source.get("message");
            Object matchedRuleIdsObj=source.get("matchedRuleIds");
            Object matchedRuleNamesObj=source.get("matchedRuleNames");
            List<Integer> matchedRuleIds;
            if (matchedRuleIdsObj instanceof List) {
                matchedRuleIds = (List<Integer>) matchedRuleIdsObj;
            } 
			else {
                matchedRuleIds = Collections.emptyList();
            }
            List<String> matchedRuleNames;
            if (matchedRuleNamesObj instanceof List) {
                matchedRuleNames = (List<String>) matchedRuleNamesObj;
            } 
			else {
                matchedRuleNames = Collections.emptyList();
            }
            for (int i=0;i<matchedRuleIds.size();i++) {
                ruleIdNameMap.put(matchedRuleIds.get(i), matchedRuleNames.get(i));
            }
            LogEntry logEntry = new LogEntry(time, date, logger, level, code, message, String.join(", ", matchedRuleNames));
            for (Integer ruleId : matchedRuleIds) {
                logsByRuleId.computeIfAbsent(ruleId, k -> new ArrayList<>()).add(logEntry);
            }
        }
        Map<Integer, List<LogEntry>> pagedLogsByRuleId = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<LogEntry>> entry : logsByRuleId.entrySet()) {
            Integer ruleId = entry.getKey();
            List<LogEntry> logs = entry.getValue();
            String paramName = "pageRule_" + ruleId;
            int page = 1;
            String pageParam = req.getParameter(paramName);
            if (pageParam != null) {
                try {
                    page = Integer.parseInt(pageParam);
                    if (page < 1) {
                        page = 1;
                    }
                } 
				catch (NumberFormatException e) {
                    page = 1;
                }
            }
            int fromIndex=(page - 1)*defaultPageSize;
            int toIndex=Math.min(fromIndex+defaultPageSize,logs.size());
            List<LogEntry> pagedLogs = (fromIndex < logs.size()) ? logs.subList(fromIndex, toIndex) : Collections.emptyList();
            pagedLogsByRuleId.put(ruleId, pagedLogs);
            req.setAttribute(paramName+"_currentPage",page);
            req.setAttribute(paramName+"_totalPages",(logs.size()+defaultPageSize-1)/ defaultPageSize);
            req.setAttribute(paramName+"_totalItems",logs.size());
        }
        req.setAttribute("logsByRuleId", pagedLogsByRuleId);
        req.setAttribute("ruleIdNameMap", ruleIdNameMap);
        req.setAttribute("pageSize", defaultPageSize);
        RequestDispatcher rd = req.getRequestDispatcher("ruleLogs.jsp");
        rd.forward(req, resp);
    }
}
