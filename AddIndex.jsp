<%@ page import="javax.servlet.http.HttpSession" %>
<%
    if (session == null || session.getAttribute("username") == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
    String warningMessage = (String) request.getAttribute("warningMessage");
%>
<%
response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
response.setHeader("Pragma", "no-cache");
response.setDateHeader("Expires", 0);
%>

<!DOCTYPE html>
<html>
<head>
    <title>Add Index</title>
    <link rel="stylesheet" href="./style.css">
</head>
<body>
<h2>Add New Index</h2>
<% if (warningMessage != null) { %>
    <p style="color:red;"><%= warningMessage %></p>
<% } %>
<form action="addIndex" method="post">
    <label for="indexName">Index Name:</label><br/>
    <input type="text" id="indexName" name="indexName" placeholder="Enter index name" required/><br/><br/>
    <input type="submit" value="Add Index" />
</form>
<br>
<form action="manageindextracking.jsp" method="get" style="margin-top: 20px;">
    <button type="submit">Manage Index Tracking</button>
</form>
<br>
<form action="index.jsp">
    <button type="submit">Back to Main</button>
</form>
</body>
</html>
