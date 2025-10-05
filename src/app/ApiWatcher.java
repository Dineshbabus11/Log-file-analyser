package app;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.xcontent.XContentType;
import com.google.gson.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class ApiWatcher implements Runnable {
    private final String apiUrl;
    private final String apiKey; 
    private final String esIndex;
    private Set<String> knownKeys = new HashSet<>();
    public ApiWatcher(String apiUrl, String apiKey, String esIndex) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.esIndex = esIndex;
    }
    public void run() {
        Gson gson = new Gson();
        SimpleRulesCache.reload();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                if (apiKey != null && !apiKey.isEmpty()) {
                    conn.setRequestProperty("X-API-KEY", apiKey);
                }

                JsonArray logs = gson.fromJson(new InputStreamReader(conn.getInputStream(), "UTF-8"), JsonArray.class);

                for (JsonElement elem : logs) {
                    JsonObject logEntry = elem.getAsJsonObject();
                    String date = logEntry.has("date") ? logEntry.get("date").getAsString() : "";
                    String time = logEntry.has("time") ? logEntry.get("time").getAsString() : "";
                    String logKey = date + "_" + time;
                    if (!knownKeys.contains(logKey)) {
                        List<Integer> matchedRuleIds = new ArrayList<>();
                        List<String> matchedRuleNames = new ArrayList<>();
                        for (SimpleRulesCache.CachedRule rule : SimpleRulesCache.getRules()) {
                            if (matchesRuleCached(rule, logEntry)) {
                                matchedRuleIds.add(rule.id);
                                matchedRuleNames.add(rule.name);
                            }
                        }
                        if (!matchedRuleIds.isEmpty()) {
                            Map<String, Object> doc = new HashMap<>();
                            doc.put("apiUrl", apiUrl);
                            doc.put("time", time);
                            doc.put("date", date);
                            doc.put("logger", logEntry.has("logger") ? logEntry.get("logger").getAsString() : "");
                            doc.put("level", logEntry.has("level") ? logEntry.get("level").getAsString() : "");
                            doc.put("code", logEntry.has("code") ? logEntry.get("code").getAsString() : "");
                            doc.put("message", logEntry.has("message") ? logEntry.get("message").getAsString() : "");
                            doc.put("matchedRuleIds", matchedRuleIds);
                            doc.put("matchedRuleNames", matchedRuleNames);
                            doc.put("timestamp", new java.util.Date());
                            RestHighLevelClient client = ESClient.getClient();
                            client.index(new IndexRequest(esIndex).source(doc, XContentType.JSON), RequestOptions.DEFAULT);
                            knownKeys.add(logKey);
                        }
                    }
                }
                conn.disconnect();
                Thread.sleep(2000);
            } 
			catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } 
			catch (Exception e) {
                e.printStackTrace();
                try { 
					Thread.sleep(4000); 
				} 
				catch (InterruptedException ignored) { 
					break; 
				}
            }
        }
    }

    private boolean matchesRuleCached(SimpleRulesCache.CachedRule rule, JsonObject entry) {
        boolean result = false;
        boolean first = true;
        for (SimpleRulesCache.RuleCondition cond : rule.conditions) {
            String fieldValue = entry.has(cond.field) ? entry.get(cond.field).getAsString() : "";
            boolean match = false;
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
                result = match; first = false;
            } 
			else {
                if ("AND".equalsIgnoreCase(cond.logicOp)){
					result = result && match;
				}
                else if ("OR".equalsIgnoreCase(cond.logicOp)){
					result = result || match;
				}
            }
        }
        return result;
    }
}
