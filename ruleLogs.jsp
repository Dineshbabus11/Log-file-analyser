<%@ page import="java.util.*" %>
<%@ page import="app.LogEntry" %>
<!DOCTYPE html>
<html>
<head>
    <title>Logs Grouped By Rule</title>
    <link rel="stylesheet" href="style.css" />
</head>
<body>
<h2>Logs Grouped By Rule</h2>
<%
    Map<Integer, List<LogEntry>> logsByRuleId = (Map<Integer, List<LogEntry>>) request.getAttribute("logsByRuleId");
    Map<Integer, String> ruleIdNameMap = (Map<Integer, String>) request.getAttribute("ruleIdNameMap");
    int pageSize=(Integer)request.getAttribute("pageSize");
    if(logsByRuleId == null||logsByRuleId.isEmpty()) {
%>
    <p>No logs found.</p>
<%
    } 
	else {
        for (Map.Entry<Integer, List<LogEntry>> entry : logsByRuleId.entrySet()) {
            Integer ruleId = entry.getKey();
            List<LogEntry> logs = entry.getValue();
            String ruleName = ruleIdNameMap.getOrDefault(ruleId, "Rule ID: " + ruleId);
            String paramName = "pageRule_" + ruleId;
            Integer currentPage = (Integer) request.getAttribute(paramName + "_currentPage");
            Integer totalPages = (Integer) request.getAttribute(paramName + "_totalPages");
            Integer totalItems = (Integer) request.getAttribute(paramName + "_totalItems");
            if (currentPage == null){
				currentPage = 1;
			} 
            if (totalPages == null){
				totalPages = 1;
			} 
            if (totalItems == null){
				totalItems = 0;
			} 
%>
    <h3>Rule: <%= ruleName %></h3>
    <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
        <thead>
            <tr>
                <th>Time</th>
                <th>Date</th>
                <th>Logger</th>
                <th>Level</th>
                <th>Code</th>
                <th>Message</th>
            </tr>
        </thead>
        <tbody>
        <%
            for (LogEntry log : logs) {
        %>
            <tr>
                <td><%= log.time %></td>
                <td><%= log.date %></td>
                <td><%= log.logger %></td>
                <td><%= log.level %></td>
                <td><%= log.code %></td>
                <td><%= log.message %></td>
            </tr>
        <%
            }
        %>
        </tbody>
    </table>
    <form method="get" action="ruleLogs" style="display:inline;">
        <input type="hidden" name="<%= paramName %>" value="<%= (currentPage > 1) ? currentPage - 1 : 1 %>" />
        <input type="submit" value="Previous" <%= (currentPage == 1) ? "disabled" : "" %> />
    </form>
    <form method="get" action="ruleLogs" style="display:inline;">
        <input type="hidden" name="<%= paramName %>" value="<%= (currentPage < totalPages) ? currentPage + 1 : totalPages %>" />
        <input type="submit" value="Next" <%= (currentPage == totalPages) ? "disabled" : "" %> />
    </form>
    <p>
        Page <%= currentPage %> of <%= totalPages %>,
        Total logs: <%= totalItems %>
    </p>
    <hr/>
<%
        }
    }
%>
</body>
</html>
