package app;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;

@WebServlet("/api/log")
public class LogIngestServlet extends HttpServlet {

    private Gson gson = new Gson();

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String indexName = req.getParameter("indexName");
        String token = req.getParameter("token").trim();

        System.out.println("Received log ingest request for index: " + indexName);

        if (indexName == null || token == null || indexName.isEmpty() || token.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"error\":\"indexName and token are required\"}");
            return;
        }

        try {
            if (!isTokenValid(indexName, token)) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().write("{\"error\":\"Invalid or expired token\"}");
                return;
            }
        } 
		catch (SQLException e) {
            throw new ServletException("Token validation error", e);
        }

        JsonObject jsonLog;
        try (BufferedReader reader = req.getReader()) {
            jsonLog = gson.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"error\":\"Invalid JSON body\"}");
            return;
        }

        if (jsonLog == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"error\":\"Empty log body\"}");
            return;
        }

        System.out.println("Log content: " + jsonLog.toString());

        SimpleRulesCache.reload();
        List<Integer> matchedRuleIds = new ArrayList<>();
        List<String> matchedRuleNames = new ArrayList<>();

        for (SimpleRulesCache.CachedRule rule : SimpleRulesCache.getRules()) {
            if (matchesRuleCached(rule, jsonLog)) {
                matchedRuleIds.add(rule.id);
                matchedRuleNames.add(rule.name);
            }
        }

        if (matchedRuleIds.isEmpty()) {
            System.out.println("No matching rules, discarding log");
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write("{\"message\":\"No matching rules, log discarded.\"}");
            return;
        }

        System.out.println("Matched rules found: " + matchedRuleNames);

        try {
            RestHighLevelClient client = ESClient.getClient();

            Map<String, Object> doc = new HashMap<>();

            for (Map.Entry<String, JsonElement> entry : jsonLog.entrySet()) {
                if (!entry.getValue().isJsonNull()) {
                    doc.put(entry.getKey(), gson.fromJson(entry.getValue(), Object.class));
                }
            }

            doc.put("matchedRuleIds", matchedRuleIds);
            doc.put("matchedRuleNames", matchedRuleNames);
            doc.put("timestamp", Instant.now().toString());

            IndexRequest reqIndex = new IndexRequest(indexName).source(doc, XContentType.JSON);
            IndexResponse response = client.index(reqIndex, RequestOptions.DEFAULT);

            System.out.println("Document indexed with ID: " + response.getId());

            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write("{\"message\":\"Log indexed successfully\"}");
        } 
		catch (Exception e) {
            System.err.println("Error during indexing: " + e.getMessage());
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("{\"error\":\"Failed to index log\"}");
        }
    }

    private boolean isTokenValid(String indexName, String token) throws SQLException {
        try (Connection con = DBconnect.connect();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT expires_at FROM index_tokens WHERE index_name = ? AND token_value = ?")) {
            ps.setString(1, indexName);
            ps.setString(2, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp expiry = rs.getTimestamp("expires_at");
                    return expiry != null && expiry.toInstant().isAfter(Instant.now());
                }
                return false;
            }
        }
    }

    private boolean matchesRuleCached(SimpleRulesCache.CachedRule rule, JsonObject log) {
        boolean result = false;
        boolean first = true;

        for (SimpleRulesCache.RuleCondition cond : rule.conditions) {
            String field = cond.field.toLowerCase();
            String fieldValue = "";

            if (log.has(field) && !log.get(field).isJsonNull()) {
                fieldValue = log.get(field).getAsString();
            }

            boolean match = false;

            if ("date".equals(field) || "time".equals(field)) {
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
            } else {
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
}
