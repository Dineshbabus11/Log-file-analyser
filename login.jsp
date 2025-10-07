<%@ page import="java.util.*" %>
<%@ page import="javax.servlet.http.*" %>
<html>
<head>
    <title>Login</title>
	<link rel="stylesheet" href="./style.css">
</head>
<body>
    <h2>Login</h2>
    <form action="login" method="post">
        Username: <input type="text" name="username" /><br/>
        Password: <input type="password" name="password" /><br/>
        <input type="submit" value="Login" />
    </form>
    <p style="color:red;"><%= request.getAttribute("error") != null ? request.getAttribute("error") : "" %></p>
    <a href="register.jsp">Register</a>
</body>
</html>
