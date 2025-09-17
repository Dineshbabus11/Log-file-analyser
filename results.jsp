<%@ page import="java.sql.ResultSet" %>
<!DOCTYPE html>
<html>
<head>
    <title>Log Analysis Results</title>
    <link rel="stylesheet" href="./style.css" />
</head>
<body>
<h2>Analysis of: ${fileName}</h2>
<p>Total Lines: ${totalLines}</p>
<p>Errors: ${errorCount}</p>
<p>Warnings: ${warningCount}</p>
<p>Info Logs: ${infoCount}</p>

<h3>Parsed Log Entries:</h3>
<table border="1" cellpadding="5">
    <tr>
        <th>Time</th><th>Date</th><th>Logger</th><th>Level</th><th>Code</th><th>Message</th>
    </tr>
    <%
        ResultSet rs = (ResultSet) request.getAttribute("logResultSet");
        if (rs != null) {
            while (rs.next()) {
    %>
    <tr>
        <td><%= rs.getString("log_time") %></td>
        <td><%= rs.getString("log_date") %></td>
        <td><%= rs.getString("logger") %></td>
        <td><%= rs.getString("level") %></td>
        <td><%= rs.getString("code") %></td>
        <td><%= rs.getString("message") %></td>
    </tr>
    <%
            }
        }
    %>
</table>

<div style="margin-top:20px;">
    <%
        Integer currentPageObj = (Integer) request.getAttribute("currentPage");
        Integer totalPagesObj = (Integer) request.getAttribute("totalPages");
        int currentPage = (currentPageObj != null) ? currentPageObj : 1;
        int totalPages = (totalPagesObj != null) ? totalPagesObj : 1;
        String filename = (String) request.getAttribute("fileName");
        String baseUrl = "uploadLog?fileName=" + filename;

        if (currentPage > 1) {
    %>
        <a href="<%= baseUrl %>&page=<%= currentPage - 1 %>">Previous</a>
    <% } %>
    Page <%= currentPage %> of <%= totalPages %>
    <% if (currentPage < totalPages) { %>
        <a href="<%= baseUrl %>&page=<%= currentPage + 1 %>">Next</a>
    <% } %>
</div>
</body>
</html>
