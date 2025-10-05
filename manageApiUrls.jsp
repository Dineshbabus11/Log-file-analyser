<%@ page import="app.DBconnect" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<html>
<head>
    <title>Manage APIs</title>
    <link rel="stylesheet" href="./style.css">
</head>
<body>
    <h2>Manage Tracked APIs</h2>
    <table border="1">
        <tr>
            <th>API URL</th>
            <th>ES Index</th>
            <th>Actions</th>
        </tr>
        <%
        try (Connection con = DBconnect.connect();
             PreparedStatement ps = con.prepareStatement("SELECT api_url, index_name, enabled FROM watched_apis");
             ResultSet rs = ps.executeQuery()) {
            while(rs.next()) {
        %>
        <tr>
            <td><%= rs.getString("api_url") %></td>
            <td><%= rs.getString("index_name") %></td>
            <td>
                <form action="viewApiLog" method="get" style="display:inline;">
                    <input type="hidden" name="indexName" value="<%= rs.getString("index_name") %>"/>
                    <button type="submit">View</button>
                </form>
                <form action="stopApiTracking" method="post" style="display:inline;">
                    <input type="hidden" name="apiUrl" value="<%= rs.getString("api_url") %>"/>
                    <button type="submit">Stop</button>
                </form>
            </td>
        </tr>
        <%
           }
        } 
		catch(Exception e){ 
			e.printStackTrace(); 
		}
        %>
    </table>
    <form action="AddApiUrl.jsp"><button type="submit">Back</button></form>
</body>
</html>
