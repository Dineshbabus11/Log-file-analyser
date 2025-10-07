<%@ page import="java.sql.*,app.DBconnect" %>
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
<html>
<head>
<title>Upload Log</title>
<link rel="stylesheet" href="./style.css">
</head>
<body>
<h2>Upload Log File</h2>
<form action="uploadLog" method="post" enctype="multipart/form-data">
	<label for="logFile">Upload File:</label>
	<input type="file" name="logFile" id="logFile" required><br><br>
	<input type="submit" value="Upload">
</form>
</body>
</html>
