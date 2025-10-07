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
    <meta charset="UTF-8" />
    <title>Multiple Application Tracking</title>
    <link rel="stylesheet" href="./style.css" />
</head>
<body>
    <h1>Multiple Application Tracking</h1>
    <h2>Add New Path</h2>
    <form action="addMultiAppPath" method="post">
        <label for="newPath">Path:</label><br/>
        <input type="text" id="newPath" name="newPath" placeholder="Enter new path" required style="width:300px;" /><br/><br/>
        <input type="submit" value="Add Path" />
    </form>
    <hr/>
    <form action="manageMultiAppPaths.jsp" method="get">
        <button type="submit">Go to Manage Paths</button>
    </form>
	<form action="index.jsp">
        <button type="submit">Back to main page</button>
    </form>
</body>
</html>
