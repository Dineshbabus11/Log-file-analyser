<%@ page import="org.elasticsearch.client.RestHighLevelClient" %>
<%@ page import="org.elasticsearch.action.search.SearchRequest" %>
<%@ page import="org.elasticsearch.search.builder.SearchSourceBuilder" %>
<%@ page import="org.elasticsearch.action.search.SearchResponse" %>
<%@ page import="org.elasticsearch.search.SearchHit" %>
<%@ page import="org.elasticsearch.client.RequestOptions" %>
<%@ page import="java.util.*" %>
<%@ page import="app.ESClient" %>
<!DOCTYPE html>
<html>
<head>
    <title>Manage Tracked Log Files</title>
    <link rel="stylesheet" href="./style.css" />
</head>
<body>
    <h1>Tracked Log Files</h1>
    <ul>
<%
    RestHighLevelClient client = ESClient.getClient();
    SearchRequest req = new SearchRequest("paths");
    req.source(new SearchSourceBuilder().size(100));
    SearchResponse resp = client.search(req, RequestOptions.DEFAULT);
    for (SearchHit hit : resp.getHits()) {
        Map<String, Object> map = hit.getSourceAsMap();
        String filePath = (String) map.get("pathString");
        if (filePath == null){
			continue;
		}
        String safeIndex = "path_" + filePath.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
%>
    <li>
        <%= filePath %>
        <form action="viewMultiAppLog" method="get" style="display:inline;">
            <input type="hidden" name="indexName" value="<%= safeIndex %>" />
            <button type="submit">View Live Logs</button>
        </form>
    </li>
<%
    }
%>
</ul>
    <a href="MultiApp.jsp">Back to Add New Log File Path</a>
</body>
</html>
