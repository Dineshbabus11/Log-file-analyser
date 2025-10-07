<%@ page import="java.util.*" %>
<%@ page import="javax.servlet.http.*" %>
<html>
<head>
    <title>Register</title>
	<link rel="stylesheet" href="./style.css">
</head>
<body>
    <h2>Register</h2>
    <form action="register" method="post">
        Username: <input type="text" name="username" /><br/>
        Password: <input type="password" name="password" /><br/>
        Role: <select name="role">
            <option value="user">User</option>
            <option value="admin">Admin</option>
        </select><br/>
        <input type="submit" value="Register" />
    </form>
    <p style="color:red;"><%= request.getAttribute("error") != null ? request.getAttribute("error") : "" %></p>
    <a href="login.jsp">Login</a>
</body>
</html>