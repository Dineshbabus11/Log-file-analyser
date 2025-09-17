<%@ page import="java.sql.*,app.DBconnect" %>
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
