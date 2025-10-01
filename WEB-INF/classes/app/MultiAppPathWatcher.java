package app;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.time.Instant;

public class MultiAppPathWatcher implements Runnable{
    private final String filePath;
    private final String esIndex;
    private long lastFilePointer=0;

    public MultiAppPathWatcher(String filePath, String esIndex){
        this.filePath=filePath;
        this.esIndex=esIndex;
    }

    public void run(){
        File logFile=new File(filePath);
		if (lastFilePointer == 0 && logFile.exists()) {
			lastFilePointer = logFile.length();
		}
        Date lastStoredTimestamp=getLastStoredTimestamp();
        SimpleRulesCache.reload();
        while(!Thread.currentThread().isInterrupted()){
            try(RandomAccessFile raf=new RandomAccessFile(logFile,"r")) {
                raf.seek(lastFilePointer);
                String line;
                while((line=raf.readLine())!=null) {
                    LogEntry logEntry=ParseMatchLog.parseLogLine(line);
                    if (logEntry == null) {
                        continue;
                    }
                    Date logDateTime = parseLogDateTime(logEntry);
                    if (lastStoredTimestamp != null && logDateTime != null && !logDateTime.after(lastStoredTimestamp)) {
                        continue;
                    }
                    List<Integer> matchedRuleIds=new ArrayList<>();
                    List<String> matchedRuleNames=new ArrayList<>();
                    for(SimpleRulesCache.CachedRule rule:SimpleRulesCache.getRules()){
                        if(matchesRuleCached(rule,logEntry)){
                            matchedRuleIds.add(rule.id);
                            matchedRuleNames.add(rule.name);
                        }
                    }
                    if(!matchedRuleIds.isEmpty()){
                        indexLogLine(logEntry,matchedRuleIds,matchedRuleNames);
                        lastStoredTimestamp=logDateTime;
                    }
                }
                lastFilePointer=raf.getFilePointer();
                Thread.sleep(2000);
            } 
			catch(InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            } 
			catch(Exception e){
                e.printStackTrace();
                try{
                    Thread.sleep(5000);
                } 
				catch(InterruptedException ex){
                    break;
                }
            }
        }
    }

    private boolean matchesRuleCached(SimpleRulesCache.CachedRule rule, LogEntry entry) {
        boolean result=false;
        boolean first=true;
        for(SimpleRulesCache.RuleCondition cond : rule.conditions) {
            String fieldValue="";
            switch (cond.field.toLowerCase()) {
                case "time": 
					fieldValue=entry.time; 
					break;
                case "date": 
					fieldValue=entry.date; 
					break;
                case "logger": 
					fieldValue=entry.logger; 
					break;
                case "level": 
					fieldValue=entry.level; 
					break;
                case "code": 
					fieldValue=entry.code; 
					break;
                case "message": 
					fieldValue=entry.message; 
					break;
            }
            boolean match=false;
            if ("date".equals(cond.field) || "time".equals(cond.field)) {
                switch (cond.operator) {
                    case "equals": 
						match = fieldValue.equals(cond.pattern); 
						break;
                    case "greater": 
						match = fieldValue.compareTo(cond.pattern) > 0; 
						break;
                    case "less": 
						match = fieldValue.compareTo(cond.pattern) < 0; 
						break;
                    case "greater_equal": 
						match = fieldValue.compareTo(cond.pattern) >= 0; 
						break;
                    case "less_equal": 
						match = fieldValue.compareTo(cond.pattern) <= 0; 
						break;
                }
            } 
			else {
                match = Pattern.matches(cond.pattern, fieldValue);
            }
            if (first) {
                result = match;
                first = false;
            } 
			else {
                if ("AND".equalsIgnoreCase(cond.logicOp)) {
                    result = result && match;
                } 
				else if ("OR".equalsIgnoreCase(cond.logicOp)) {
                    result = result || match;
                }
            }
        }
        return result;
    }

    private void indexLogLine(LogEntry entry, List<Integer> matchedRuleIds, List<String> matchedRuleNames) {
        try {
            RestHighLevelClient client = ESClient.getClient();
            Map<String, Object> doc = new HashMap<>();
            doc.put("fileName", filePath);
            doc.put("time", entry.time);
            doc.put("date", entry.date);
            doc.put("logger", entry.logger);
            doc.put("level", entry.level);
            doc.put("code", entry.code);
            doc.put("message", entry.message);
            doc.put("matchedRuleIds", matchedRuleIds);
            doc.put("matchedRuleNames", matchedRuleNames);
            doc.put("timestamp", new java.util.Date());
            IndexRequest req = new IndexRequest(esIndex).source(doc, XContentType.JSON);
            client.index(req, RequestOptions.DEFAULT);
        } 
		catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Date getLastStoredTimestamp(){
        try{
            RestHighLevelClient client=ESClient.getClient();
            SearchRequest searchRequest=new SearchRequest(esIndex);
            SearchSourceBuilder sourceBuilder=new SearchSourceBuilder()
                    .query(QueryBuilders.matchQuery("fileName", filePath)).sort("timestamp", SortOrder.DESC).size(1);
            searchRequest.source(sourceBuilder);
            SearchResponse response=client.search(searchRequest, RequestOptions.DEFAULT);
            if(response.getHits().getHits().length>0) {
                Map<String,Object> source=response.getHits().getAt(0).getSourceAsMap();
                Object ts=source.get("timestamp");
                if(ts!=null){
                    Instant instant=Instant.parse(ts.toString());
                    return Date.from(instant);
                }
            }
        } 
		catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private Date parseLogDateTime(LogEntry entry){
        try{
            String dateTime=entry.date+" "+entry.time;
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            return sdf.parse(dateTime);
        } 
		catch(Exception e){
            return null;
        }
    }
}
