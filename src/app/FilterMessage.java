package app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

@WebServlet("/filterMessage")
public class FilterMessage extends HttpServlet{
    private static final int size=10;
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException{
		SessionUtils.setNoCacheHeaders(res);
		if (!SessionUtils.checkLogin(req, res)) {
			return;
		}
        String msgPattern=req.getParameter("msgPattern");
        String fileName=req.getParameter("fileName");

        int page=1;
        String pageParam=req.getParameter("page");
        if(pageParam!=null){
            try{
                page=Integer.parseInt(pageParam);
                if(page < 1){
                    page=1;
                }
            } 
			catch(NumberFormatException ignored){}
        }
        int offset=(page-1)*size;
        List<LogEntry> esLogs=new ArrayList<>();
        int errorCount=0, warningCount=0, infoCount=0;
        long totalHits=0;
        RestHighLevelClient client=ESClient.getClient();

        try{
            SearchRequest searchRequest=new SearchRequest("logs");
            SearchSourceBuilder sourceBuilder=new SearchSourceBuilder();
            String regexPattern=(msgPattern!=null && !msgPattern.trim().isEmpty())?msgPattern:".*";
            if(fileName!=null&&!fileName.trim().isEmpty()){
                sourceBuilder.query(QueryBuilders.boolQuery().must(QueryBuilders.regexpQuery("message.keyword",".*"+regexPattern+".*"))
                        .filter(QueryBuilders.termQuery("filename.keyword", fileName)));
            } 
			else{
                sourceBuilder.query(QueryBuilders.regexpQuery("message.keyword", ".*" + regexPattern + ".*"));
            }

            sourceBuilder.from(offset);
            sourceBuilder.size(size);
            searchRequest.source(sourceBuilder);

            SearchResponse searchResponse=client.search(searchRequest, RequestOptions.DEFAULT);
            totalHits=searchResponse.getHits().getTotalHits().value;

            for(SearchHit hit:searchResponse.getHits().getHits()) {
                Map<String,Object> sourceMap=hit.getSourceAsMap();
                String time=(String)sourceMap.get("time");
                String date=(String)sourceMap.get("date");
                String logger=(String)sourceMap.get("logger");
                String level=(String)sourceMap.get("level");
                String code=(String)sourceMap.get("code");
                String message=(String)sourceMap.get("message");
                String matchedRuleNames = "";
                Object matchedRuleNamesObj = sourceMap.get("matchedRuleNames");
                if (matchedRuleNamesObj instanceof List) {
                    List<String> ruleNames=(List<String>) matchedRuleNamesObj;
                    matchedRuleNames=String.join(", ",ruleNames);
                }
                LogEntry le=new LogEntry(time, date, logger, level, code, message, matchedRuleNames);
                esLogs.add(le);

                if("ERROR".equalsIgnoreCase(level)){
                    errorCount++;
                } 
				else if("WARN".equalsIgnoreCase(level)||"WARNING".equalsIgnoreCase(level)){
                    warningCount++;
                }
				else if("INFO".equalsIgnoreCase(level)){
                    infoCount++;
                }
            }
        } 
		catch(Exception e){
            throw new ServletException(e);
        }

        req.setAttribute("esLogEntries",esLogs);
        req.setAttribute("fileName",fileName);
        req.setAttribute("totalLines",totalHits);
        req.setAttribute("errorCount",errorCount);
        req.setAttribute("warningCount",warningCount);
        req.setAttribute("infoCount",infoCount);
        req.setAttribute("currentPage",page);
        req.setAttribute("totalPages",(int)Math.ceil((double)totalHits/size));
        RequestDispatcher rd=req.getRequestDispatcher("results.jsp");
        rd.forward(req,res);
    }
}
