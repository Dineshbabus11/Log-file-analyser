<%@ page import="java.sql.*, app.DBconnect" %>
<%@ page import="java.util.*" %>
<%
    if (session == null || session.getAttribute("username") == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
    Map<String, List<Map<String, Object>>> indexTokensMap = new LinkedHashMap<>();
    try (Connection con = DBconnect.connect();
         PreparedStatement psIndex = con.prepareStatement("SELECT index_name FROM tracked_indices WHERE enabled = TRUE ORDER BY created_at DESC");
         ResultSet rsIndex = psIndex.executeQuery()) {
        while (rsIndex.next()) {
            String indexName = rsIndex.getString("index_name");
            List<Map<String, Object>> tokensList = new ArrayList<>();
            try (PreparedStatement psToken = con.prepareStatement(
                    "SELECT token_id, token_value, expires_at FROM index_tokens WHERE index_name = ? ORDER BY expires_at ASC")) {
                psToken.setString(1, indexName);
                try (ResultSet rsToken = psToken.executeQuery()) {
                    while (rsToken.next()) {
                        Map<String, Object> token = new HashMap<>();
                        token.put("tokenId", rsToken.getInt("token_id"));
                        token.put("tokenValue", rsToken.getString("token_value"));
                        token.put("expiresAt", rsToken.getTimestamp("expires_at"));
                        tokensList.add(token);
                    }
                }
            }
            indexTokensMap.put(indexName, tokensList);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
%>
<!DOCTYPE html>
<html>
<head>
    <title>Manage Index Tracking & Tokens</title>
    <link rel="stylesheet" href="./style.css" />
    <style>
        table {border-collapse: collapse; width: 100%;}
        th, td {border: 1px solid #ddd; padding: 8px;}
        th {background-color: #f2f2f2;}
        .token-table {margin-top: 8px; margin-bottom: 16px;}
    </style>
</head>
<body>
    <h2>Tracked Indices & Tokens</h2>
    <a href="AddIndex.jsp">Add New Index</a>
    <hr/>
    <%
        if (indexTokensMap.isEmpty()) {
    %>
        <p>No tracked indices found.</p>
    <%
        } else {
            for (Map.Entry<String, List<Map<String, Object>>> entry : indexTokensMap.entrySet()) {
                String indexName = entry.getKey();
                List<Map<String, Object>> tokens = entry.getValue();
    %>
        <h3>Index: <%= indexName %></h3>
        <form action="viewIndex" method="get" style="display:inline-block; margin-bottom: 12px;">
            <input type="hidden" name="indexName" value="<%= indexName %>" />
            <button type="submit">View Index Logs</button>
        </form>
        <form action="addToken" method="post" style="display:inline-block; margin-bottom:12px; margin-left:10px;">
            <input type="hidden" name="indexName" value="<%= indexName %>" />
            <button type="submit">Add Token</button>
        </form>
        <table class="token-table">
            <thead>
                <tr>
                    <th>Token</th>
                    <th>Expires At</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>
            <%
                if (tokens.isEmpty()) {
            %>
                <tr><td colspan="3">No tokens generated.</td></tr>
            <%
                } else {
                    for (Map<String, Object> token : tokens) {
            %>
                <tr>
                    <td><%= token.get("tokenValue") %></td>
                    <td><%= token.get("expiresAt") %></td>
                    <td>
                        <form method="post" action="deleteToken" onsubmit="return confirm('Delete token?');" style="display:inline;">
                            <input type="hidden" name="tokenId" value="<%= token.get("tokenId") %>"/>
                            <input type="hidden" name="indexName" value="<%= indexName %>"/>
                            <button type="submit">Delete</button>
                        </form>
                    </td>
                </tr>
            <%
                    }
                }
            %>
            </tbody>
        </table>
    <%
            }
        }
    %>
</body>
</html>
