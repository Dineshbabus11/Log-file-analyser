package app;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;

@WebServlet("/ruleLogs")
public class RuleLogsServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		SessionUtils.setNoCacheHeaders(resp);
		if (!SessionUtils.checkLogin(req, resp)) {
			return;
		}
        RestHighLevelClient esClient=ESClient.getClient();
        List<String> allLogIds=new ArrayList<>();
        Map<String, List<Integer>> logIdToRuleIds=new HashMap<>();
        Map<Integer, String> ruleIdNameMap=new HashMap<>();
        int defaultPageSize=10;
        try (Connection con=DBconnect.connect();
             PreparedStatement ps=con.prepareStatement("SELECT lrm.log_id, r.id AS rule_id, r.name AS rule_name "+
                     "FROM log_rule_map lrm "+"JOIN rules r ON lrm.rule_id = r.id "+"ORDER BY r.id")) {
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                String logId = rs.getString("log_id");
                int ruleId = rs.getInt("rule_id");
                String ruleName = rs.getString("rule_name");
                ruleIdNameMap.putIfAbsent(ruleId,ruleName);
                logIdToRuleIds.computeIfAbsent(logId,k->new ArrayList<>()).add(ruleId);
                if (!allLogIds.contains(logId)) {
                    allLogIds.add(logId);
                }
            }
        } 
		catch(SQLException e){
            throw new ServletException(e);
        }
        Map<Integer,List<LogEntry>> logsByRuleId=new LinkedHashMap<>();
        for(Integer ruleId:ruleIdNameMap.keySet()) {
            logsByRuleId.put(ruleId,new ArrayList<>());
        }
        MultiGetRequest mgetRequest=new MultiGetRequest();
        for(String logId:allLogIds){
            mgetRequest.add(new MultiGetRequest.Item("logs",logId));
        }
        try{
            MultiGetResponse mgetResponse=esClient.mget(mgetRequest,RequestOptions.DEFAULT);
            for(MultiGetItemResponse itemResponse:mgetResponse.getResponses()){
                if (!itemResponse.isFailed()) {
                    GetResponse getResponse=itemResponse.getResponse();
                    if (getResponse.isExists()) {
                        String logId=getResponse.getId();
                        Map<String,Object> source=getResponse.getSourceAsMap();
                        String time=(String)source.get("time");
                        String date=(String)source.get("date");
                        String logger=(String)source.get("logger");
                        String level=(String)source.get("level");
                        String code=(String)source.get("code");
                        String message=(String)source.get("message");
                        List<Integer> associatedRuleIds=logIdToRuleIds.get(logId);
                        if(associatedRuleIds!=null){
                            for(Integer ruleId:associatedRuleIds){
                                LogEntry logEntry=new LogEntry(time,date,logger,level,code,message,ruleIdNameMap.get(ruleId));
                                logsByRuleId.get(ruleId).add(logEntry);
                            }
                        }
                    }
                }
            }
        } 
		catch(IOException e){
            throw new ServletException(e);
        }
        Map<Integer, List<LogEntry>> pagedLogsByRuleId=new LinkedHashMap<>();
        for(Map.Entry<Integer, List<LogEntry>> entry:logsByRuleId.entrySet()) {
            Integer ruleId=entry.getKey();
            List<LogEntry> logs=entry.getValue();

            String paramName="pageRule_"+ruleId;
            int page=1;
            String pageParam=req.getParameter(paramName);
            if(pageParam!=null){
                try{
                    page=Integer.parseInt(pageParam);
                    if(page<1){
						page=1;
					}
                } 
				catch(NumberFormatException e){
                    page=1;
                }
            }
            int fromIndex=(page-1)*defaultPageSize;
            int toIndex=Math.min(fromIndex+defaultPageSize,logs.size());
            List<LogEntry> pagedLogs=(fromIndex<logs.size())?logs.subList(fromIndex, toIndex):Collections.emptyList();
            pagedLogsByRuleId.put(ruleId, pagedLogs);
            req.setAttribute(paramName + "_currentPage", page);
            req.setAttribute(paramName + "_totalPages", (logs.size() + defaultPageSize - 1) / defaultPageSize);
            req.setAttribute(paramName + "_totalItems", logs.size());
        }
        req.setAttribute("logsByRuleId", pagedLogsByRuleId);
        req.setAttribute("ruleIdNameMap", ruleIdNameMap);
        req.setAttribute("pageSize", defaultPageSize);

        RequestDispatcher rd = req.getRequestDispatcher("ruleLogs.jsp");
        rd.forward(req, resp);
    }
}
