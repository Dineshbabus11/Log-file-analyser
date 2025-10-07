<%@page import="java.sql.*,app.DBconnect"%>
<%@ page import="javax.servlet.http.HttpSession" %>
<%
    if (session == null || session.getAttribute("username") == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
%>

<!DOCTYPE html>
<HTML>
	<head>
<title>Upload Path</title>
<link rel="stylesheet" href="./style.css">
</head>
<body>
<h2>Upload Path</h2>
<form action="uploadPath" method="post">
	<label for="path">Upload File:</label>
	<input type="text" name="path" id="path" required><br><br>
	<input type="submit" value="Upload">
</form>
</body>
</HTML>