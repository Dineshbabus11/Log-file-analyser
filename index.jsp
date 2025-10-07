<%@ page import="javax.servlet.http.HttpSession" %>
<%
    if (session == null || session.getAttribute("username") == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
%>
<%
response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
response.setHeader("Pragma", "no-cache");
response.setDateHeader("Expires", 0);
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="./style.css">
    <title>Log Analyser</title>
</head>
<body>
    <h1>Log Analyser</h1>
    <ul>
        <li><a href="uploadForm.jsp">Upload Log</a></li>
        <li><a href="addRule.jsp">Add Rule</a></li>
        <li><a href="viewRules">View Rules</a></li>
        <li><a href="UploadPath.jsp">Track Application</a></li>
        <li><a href="MultiApp.jsp">Multiple Application Tracking</a></li>
        <li><a href="AddIndex.jsp">Index Tracking</a></li>
    </ul>

    <form action="logout" method="get" style="margin-top: 20px;">
        <input type="submit" value="Logout" />
    </form>
</body>
</html>
