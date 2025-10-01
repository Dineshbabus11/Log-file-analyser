package app;

import java.io.*;
import java.util.*;
import java.sql.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import org.elasticsearch.action.index.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;

@WebServlet("/uploadLog")
@MultipartConfig
public class UploadFile extends HttpServlet {
	private static final int size = 10;

	protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException {
		doPost(req,res);
	}

	protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException {
		String contentType = req.getContentType();
		Part filePart=null;
		if(contentType!=null && contentType.toLowerCase().startsWith("multipart/")) {
			filePart=req.getPart("logFile");
		}
		String fileName=null;
		if(filePart!=null){
			fileName=filePart.getSubmittedFileName();
			String folderPath=getServletContext().getRealPath("")+File.separator+"folder";
			File folder=new File(folderPath);
			if(!folder.exists()){
				folder.mkdir();
			}
			File file=new File(folder,fileName);
			filePart.write(file.getAbsolutePath());
		}
		else{
			fileName=req.getParameter("fileName");
			if(fileName==null || fileName.isEmpty()) {
				res.sendError(HttpServletResponse.SC_BAD_REQUEST, "No fileName provided");
				return;
			}
		}
		int totalLines = 0, errorCount = 0, warningCount = 0, infoCount = 0;
		try(Connection con=DBconnect.connect()) {
			RestHighLevelClient client=ESClient.getClient();
			DeleteByQueryRequest deleteRequest=new DeleteByQueryRequest("logs");
			deleteRequest.setQuery(new MatchAllQueryBuilder());
			deleteRequest.setConflicts("proceed");
			BulkByScrollResponse deleteResponse=client.deleteByQuery(deleteRequest, RequestOptions.DEFAULT);
			String folderPath=getServletContext().getRealPath("")+File.separator+"folder";
			File logFile=new File(folderPath,fileName);
			try(Statement st=con.createStatement()){
				st.executeUpdate("TRUNCATE TABLE log_rule_map RESTART IDENTITY");
			}
			try(BufferedReader br = new BufferedReader(new FileReader(logFile))) {
				String line;
				while((line=br.readLine())!=null) {
					totalLines++;
					LogEntry logEntry=ParseMatchLog.parseLogLine(line);
					if(logEntry==null){
						continue;
					}
					List<Integer> matchedRuleIds=new ArrayList<>();
					List<String> matchedRuleNames=new ArrayList<>();
					try(PreparedStatement psRules=con.prepareStatement("SELECT id, name FROM rules");
						ResultSet rsRules=psRules.executeQuery()) {
						while(rsRules.next()){
							int ruleId=rsRules.getInt("id");
							String ruleName=rsRules.getString("name");
							if(ParseMatchLog.matchesRule(con,ruleId,logEntry)) {
								matchedRuleIds.add(ruleId);
								matchedRuleNames.add(ruleName);
							}
						}
					}
					if(!matchedRuleIds.isEmpty()) {
						switch(logEntry.level.toUpperCase()) {
							case "ERROR":
								errorCount++;
								break;
							case "WARNING":
								warningCount++;
								break;
							case "INFO":
								infoCount++;
								break;
						}
						Map<String, Object> jsonMap=new HashMap<>();
						jsonMap.put("filename",fileName);
						jsonMap.put("time",logEntry.time);
						jsonMap.put("date",logEntry.date);
						jsonMap.put("logger",logEntry.logger);
						jsonMap.put("level",logEntry.level);
						jsonMap.put("code",logEntry.code);
						jsonMap.put("message",logEntry.message);
						jsonMap.put("matchedRuleIds",matchedRuleIds);
						jsonMap.put("matchedRuleNames",matchedRuleNames);
						jsonMap.put("timestamp", new java.util.Date());
						IndexRequest indexRequest=new IndexRequest("logs").source(jsonMap, XContentType.JSON);
						IndexResponse indexResponse=client.index(indexRequest,RequestOptions.DEFAULT);
						String esId = indexResponse.getId();
						try(PreparedStatement psInsert = con.prepareStatement("INSERT INTO log_rule_map (log_id, rule_id) VALUES (?, ?)")){
							for(Integer ruleId:matchedRuleIds) {
								psInsert.setString(1,esId);
								psInsert.setInt(2,ruleId);
								psInsert.addBatch();
							}
							psInsert.executeBatch();
						}
					}
				}
			}
			RefreshRequest refreshRequest=new RefreshRequest("logs");
            RefreshResponse refreshResponse=client.indices().refresh(refreshRequest, RequestOptions.DEFAULT);
			int page=1;
			String pageParam=req.getParameter("page");
			if(pageParam!=null) {
				try{
					page=Integer.parseInt(pageParam);
					if(page<1){
						page=1;
					} 
				} 
				catch(Exception e) {
					e.printStackTrace();
				}
			}
			ArrayList<LogEntry> esLogs = fetchLogsFromES(client, page, size);
			long totalHits = getTotalHits(client);
			req.setAttribute("esLogEntries",esLogs);
			req.setAttribute("fileName",fileName);
			req.setAttribute("totalLines",totalLines);
			req.setAttribute("errorCount",errorCount);
			req.setAttribute("warningCount",warningCount);
			req.setAttribute("infoCount",infoCount);
			req.setAttribute("currentPage",page);
			req.setAttribute("totalPages",(int)Math.ceil((double)totalHits/size));
			RequestDispatcher rd=req.getRequestDispatcher("results.jsp");
			rd.forward(req,res);
		} 
		catch(Exception e){
			throw new ServletException(e);
		}
	}

	private long getTotalHits(RestHighLevelClient client) throws IOException {
		SearchRequest searchRequest=new SearchRequest("logs");
		SearchSourceBuilder sourceBuilder=new SearchSourceBuilder();
		sourceBuilder.size(0);
		searchRequest.source(sourceBuilder);
		SearchResponse searchResponse=client.search(searchRequest, RequestOptions.DEFAULT);
		return searchResponse.getHits().getTotalHits().value;
	}

	private ArrayList<LogEntry> fetchLogsFromES(RestHighLevelClient client, int page, int size) throws IOException {
		ArrayList<LogEntry> esLogs=new ArrayList<>();
		SearchRequest searchRequest=new SearchRequest("logs");
		SearchSourceBuilder sourceBuilder=new SearchSourceBuilder();
		sourceBuilder.query(QueryBuilders.matchAllQuery());
		sourceBuilder.from((page-1)*size);
		sourceBuilder.size(size);
		searchRequest.source(sourceBuilder);
		SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
		for(SearchHit hit : searchResponse.getHits().getHits()) {
			Map<String, Object> sourceMap = hit.getSourceAsMap();
			String time = (String)sourceMap.get("time");
			String date = (String)sourceMap.get("date");
			String logger = (String)sourceMap.get("logger");
			String level = (String)sourceMap.get("level");
			String code = (String)sourceMap.get("code");
			String message = (String)sourceMap.get("message");
			String matchedRuleNames = "";
			Object matchedRuleNamesObj = sourceMap.get("matchedRuleNames");
			if(matchedRuleNamesObj instanceof List) {
				List<String> ruleNames = (List<String>) matchedRuleNamesObj;
				matchedRuleNames = String.join(", ",ruleNames);
			}
			LogEntry le = new LogEntry(time,date,logger,level,code,message,matchedRuleNames);
			esLogs.add(le);
		}
		return esLogs;
	}

	public void destroy() {
		try {
			ESClient.closeClient();
		} 
		catch(Exception e) {
			e.printStackTrace();
		}
		super.destroy();
	}
}
